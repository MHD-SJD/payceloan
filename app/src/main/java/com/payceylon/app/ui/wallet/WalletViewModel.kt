package com.payceylon.app.ui.wallet

import androidx.annotation.DrawableRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.payceylon.app.R
import com.payceylon.app.data.BackendApi
import com.payceylon.app.ui.account.AccountState
import com.payceylon.app.ui.account.AppNotification
import com.payceylon.app.ui.account.NotificationKind
import com.payceylon.app.ui.account.PaymentMethod
import com.payceylon.app.ui.account.PaymentMethodType
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject

enum class WalletType(val label: String, val transactionLabel: String) {
    Fiat(label = "Fiat Wallet", transactionLabel = "Fiat"),
    Crypto(label = "Crypto Wallet", transactionLabel = "Crypto"),
}

enum class TransactionDirection {
    Outgoing,
    Incoming,
    Swap,
    /** LKR added from a linked card or bank account. */
    TopUp,
    /** LKR paid out to a linked bank account. */
    Withdrawal,
    ;

    /** True when money left the wallet. */
    val isDebit: Boolean get() = this == Outgoing || this == Withdrawal

    /** Top-ups and withdrawals move money to or from an outside account, not a PayCeylon user. */
    val isExternal: Boolean get() = this == TopUp || this == Withdrawal
}

/**
 * Tokens held in the crypto wallet. Addresses are well-known public example addresses for
 * the prototype; real ones come from the custody / wallet provider per user.
 */
enum class CryptoToken(
    val symbol: String,
    val tokenName: String,
    val network: String,
    val address: String,
    /** Branded icon from web3icons. */
    @param:DrawableRes val icon: Int,
    /** Decimal places shown and accepted for amounts. */
    val decimals: Int,
) {
    USDT("USDT", "Tether", "TRC-20", "TQn9Y2khEsLJW1ChVWFMSMeRDow5KcbLSE", R.drawable.ic_token_usdt, 2),
    BTC("BTC", "Bitcoin", "Bitcoin", "bc1qxy2kgdygjrsqtzq2n0yrf2493p83kkfjhx0wlh", R.drawable.ic_token_btc, 6),
    ETH("ETH", "Ethereum", "ERC-20", "0x71C7656EC7ab88b098defB751B7401B5f6d8976F", R.drawable.ic_token_eth, 4),
    SOL("SOL", "Solana", "Solana", "7EcDhSYGxXyscszYEp35KHN8vvw3svAuLKTzXwCFLtV", R.drawable.ic_token_sol, 2),
}

/**
 * Anything the user can hold, send or receive: Sri Lankan rupees in the fiat wallet, or a
 * token in the crypto wallet. Payment links and transactions refer to assets by [code].
 */
enum class Asset(val code: String, val displayName: String, val token: CryptoToken?) {
    LKR("LKR", "Fiat Wallet", null),
    USDT("USDT", "Tether", CryptoToken.USDT),
    BTC("BTC", "Bitcoin", CryptoToken.BTC),
    ETH("ETH", "Ethereum", CryptoToken.ETH),
    SOL("SOL", "Solana", CryptoToken.SOL),
    ;

    val decimals: Int get() = token?.decimals ?: 2
    val wallet: WalletType get() = if (token == null) WalletType.Fiat else WalletType.Crypto

    /** Secondary line in asset pickers. */
    val detail: String get() = token?.let { "${it.symbol} \u00B7 ${it.network}" } ?: "Sri Lankan Rupee"

    companion object {
        fun of(token: CryptoToken): Asset = entries.first { it.token == token }
        fun fromCode(code: String?): Asset? = entries.firstOrNull { it.code.equals(code, ignoreCase = true) }
    }
}

/** Shortens an address to its first and last characters, e.g. "TQn9Y2…KcbLSE". */
fun CryptoToken.shortAddress(): String = "${address.take(6)}\u2026${address.takeLast(6)}"

data class PayCeylonUser(val name: String, val handle: String) {
    val initials: String
        get() = name.split(' ').filter { it.isNotBlank() }.take(2)
            .joinToString("") { it.first().uppercase() }
}

/** Registered PayCeylon users the prototype can send to. Replace with a remote user search. */
val PayCeylonDirectory = listOf(
    PayCeylonUser("Sajjad Ahamed", "sajjad67"),
    PayCeylonUser("Janidu Perera", "janidup"),
    PayCeylonUser("Ruveen Jayasinghe", "ruveenobject"),
    PayCeylonUser("Anuka Fernando", "anuka67"),
    PayCeylonUser("Dialog Axiata", "dialoglk"),
    PayCeylonUser("Kavindi Perera", "kavindi.p"),
    PayCeylonUser("Tharindu Silva", "tharindu_s"),
    PayCeylonUser("Nimesha Rathnayake", "nimesha99"),
    PayCeylonUser("Dulanjaya Wickramasinghe", "dulanjaya"),
    PayCeylonUser("Sachini Weerasekara", "sachini.w"),
    PayCeylonUser("Pasan Gunawardena", "pasan.lk"),
    PayCeylonUser("Hiruni Madushani", "hiruni_m"),
    PayCeylonUser("Keells Super", "keells"),
    PayCeylonUser("PickMe", "pickme.lk"),
)

/**
 * Matches users whose handle or name contains [query] (ignoring a leading "@"), ranking
 * handle prefixes first, then name prefixes, then other matches.
 */
fun searchPayCeylonUsers(query: String, limit: Int = 5): List<PayCeylonUser> {
    val q = query.trim().removePrefix("@").lowercase()
    if (q.isEmpty()) return emptyList()
    return PayCeylonDirectory
        .mapNotNull { user ->
            val handle = user.handle.lowercase()
            val name = user.name.lowercase()
            val rank = when {
                handle.startsWith(q) -> 0
                name.split(' ').any { it.startsWith(q) } -> 1
                q in handle || q in name -> 2
                else -> return@mapNotNull null
            }
            rank to user
        }
        .sortedWith(compareBy({ it.first }, { it.second.handle }))
        .take(limit)
        .map { it.second }
}

fun findPayCeylonUser(handle: String): PayCeylonUser? =
    PayCeylonDirectory.firstOrNull { it.handle.equals(handle.removePrefix("@"), ignoreCase = true) }

data class WalletTransaction(
    val id: Long,
    val counterpartyName: String,
    val counterpartyHandle: String,
    val direction: TransactionDirection,
    /** Asset code that left (outgoing / swap) or arrived (incoming). */
    val currency: String,
    val amount: BigDecimal,
    val wallet: WalletType,
    /** When it happened, in epoch milliseconds. */
    val timestamp: Long,
    /** Swaps only: the asset and amount received. */
    val toCurrency: String? = null,
    val toAmount: BigDecimal? = null,
    /** Fee charged on top of [amount], in [feeCurrency]: card top-up, bank payout or network fee. */
    val fee: BigDecimal? = null,
    val feeCurrency: String? = null,
    val remoteId: String? = null,
) {
    val title: String
        get() = when (direction) {
            TransactionDirection.Outgoing -> "Transfer to $counterpartyName"
            TransactionDirection.Incoming -> "Receive from $counterpartyName"
            TransactionDirection.Swap -> "Swap $currency to $toCurrency"
            TransactionDirection.TopUp -> "Top up from $counterpartyName"
            TransactionDirection.Withdrawal -> "Withdraw to $counterpartyName"
        }

    /** How this transaction changed the balance of [asset] (negative when money left). */
    fun netChange(asset: Asset): BigDecimal {
        var change = BigDecimal.ZERO
        if (currency == asset.code) {
            change += when (direction) {
                TransactionDirection.Incoming, TransactionDirection.TopUp -> amount
                TransactionDirection.Swap -> -amount
                TransactionDirection.Outgoing, TransactionDirection.Withdrawal -> -(amount + (fee ?: BigDecimal.ZERO))
            }
        }
        if (direction == TransactionDirection.Swap && toCurrency == asset.code) {
            change += toAmount ?: BigDecimal.ZERO
        }
        return change
    }

    /** Swaps touching LKR also appear in the fiat wallet. */
    fun belongsTo(wallet: WalletType): Boolean =
        if (direction == TransactionDirection.Swap) {
            listOfNotNull(Asset.fromCode(currency), Asset.fromCode(toCurrency)).any { it.wallet == wallet }
        } else {
            this.wallet == wallet
        }
}

object WalletFees {
    /** Most a single top-up can be worth, in LKR. */
    val maxTopUpLkr: BigDecimal = BigDecimal("500000")

    /** Flat CEFTS charge for paying LKR out to a bank account. */
    val withdrawalFee: BigDecimal = BigDecimal("25.00")

    /** Network fee for sending a token on-chain (fixed per token for the prototype). */
    fun networkFee(asset: Asset): BigDecimal? = when (asset) {
        Asset.USDT -> BigDecimal("1.00")
        Asset.BTC -> BigDecimal("0.00002")
        Asset.ETH -> BigDecimal("0.0004")
        Asset.SOL -> BigDecimal("0.000005")
        Asset.LKR -> null
    }

    /** What [amount] of [asset] costs in LKR at [SwapRates]. */
    fun lkrPrice(asset: Asset, amount: BigDecimal): BigDecimal =
        amount.multiply(SwapRates.rate(asset, Asset.LKR)).setScale(2, RoundingMode.UP)

    /** Fee [method] adds on top of an LKR top-up. */
    fun topUpFee(amount: BigDecimal, method: PaymentMethod): BigDecimal =
        amount.multiply(method.topUpFeeRate).setScale(2, RoundingMode.UP)
}

/**
 * Indicative prices in LKR used by the swap screen; a real build would stream these from
 * the exchange partner.
 */
object SwapRates {
    private val lkrPrice = mapOf(
        Asset.LKR to BigDecimal("1"),
        Asset.USDT to BigDecimal("300"),
        Asset.BTC to BigDecimal("30000000"),
        Asset.ETH to BigDecimal("1050000"),
        Asset.SOL to BigDecimal("45000"),
    )

    /** PayCeylon's swap fee, taken from the amount being swapped. */
    val feeRate: BigDecimal = BigDecimal("0.005")

    /** How many [to] one [from] buys, before fees. */
    fun rate(from: Asset, to: Asset): BigDecimal =
        lkrPrice.getValue(from).divide(lkrPrice.getValue(to), MathContext.DECIMAL64)

    fun fee(from: Asset, amount: BigDecimal): BigDecimal =
        amount.multiply(feeRate).setScale(from.decimals, RoundingMode.UP)

    /** What arrives for [amount] of [from], after the fee, cut to [to]'s precision. */
    fun quote(from: Asset, to: Asset, amount: BigDecimal): BigDecimal =
        (amount - fee(from, amount)).max(BigDecimal.ZERO)
            .multiply(rate(from, to))
            .setScale(to.decimals, RoundingMode.DOWN)
}

/**
 * In-memory wallet for the prototype. Swap the seeded data for a Room-cached remote
 * repository once the wallet API is available.
 */
class WalletViewModel : ViewModel() {
    private val balances = mutableStateOf(
        mapOf(
            Asset.LKR to BigDecimal("32483.75"),
            Asset.USDT to BigDecimal("1284.32"),
            Asset.BTC to BigDecimal("0.042150"),
            Asset.ETH to BigDecimal("0.8532"),
            Asset.SOL to BigDecimal("41.20"),
        ),
    )

    var selectedWallet by mutableStateOf(WalletType.Fiat)

    var selectedToken by mutableStateOf(CryptoToken.USDT)

    /** What Home shows and Send / Receive default to: LKR, or the selected crypto token. */
    val selectedAsset: Asset
        get() = if (selectedWallet == WalletType.Crypto) Asset.of(selectedToken) else Asset.LKR

    /**
     * The last balance the Home card finished showing, so it can count from there after a
     * payment instead of replaying its intro. Not UI state; read once per display.
     */
    var lastShownBalance: Pair<Asset, BigDecimal>? = null

    private val now = System.currentTimeMillis()
    private fun minutesAgo(minutes: Long) = now - minutes * 60_000

    val transactions = mutableStateListOf(
        WalletTransaction(1, "Janidu", "janidup", TransactionDirection.Outgoing, "LKR", BigDecimal("10000.00"), WalletType.Fiat, minutesAgo(12)),
        WalletTransaction(2, "Sajjad", "sajjad67", TransactionDirection.Incoming, "USDT", BigDecimal("73.54"), WalletType.Crypto, minutesAgo(40)),
        WalletTransaction(3, "Dialog", "dialoglk", TransactionDirection.Outgoing, "LKR", BigDecimal("2500.00"), WalletType.Fiat, minutesAgo(118)),
        WalletTransaction(4, "Ruveen", "ruveenobject", TransactionDirection.Outgoing, "SOL", BigDecimal("23.54"), WalletType.Crypto, minutesAgo(124)),
        WalletTransaction(5, "Anuka", "anuka67", TransactionDirection.Incoming, "LKR", BigDecimal("6700.00"), WalletType.Fiat, minutesAgo(131)),
        WalletTransaction(6, "Kavindi", "kavindi.p", TransactionDirection.Incoming, "BTC", BigDecimal("0.0125"), WalletType.Crypto, minutesAgo(302)),
        WalletTransaction(7, "Pasan", "pasan.lk", TransactionDirection.Outgoing, "ETH", BigDecimal("0.15"), WalletType.Crypto, minutesAgo(1_510)),
        WalletTransaction(8, "Keells", "keells", TransactionDirection.Outgoing, "LKR", BigDecimal("4320.00"), WalletType.Fiat, minutesAgo(1_565)),
    )

    /** Profile, linked accounts, settings and the notification feed. */
    val account = AccountState(now)

    private var pollingJob: Job? = null
    private val _incomingPaymentEvent = MutableSharedFlow<String>(extraBufferCapacity = 10)
    val incomingPaymentEvent = _incomingPaymentEvent.asSharedFlow()

    private val transactionIdMap = mutableMapOf<String, Long>()
    private var nextTransactionId = 1000L
    private val notificationIdMap = mutableMapOf<String, Long>()
    private var nextNotificationId = 1000L
    private val notifiedIncomingIds = mutableSetOf<String>()
    private var hasSyncedTransactions = false
    private var hasSyncedNotifications = false

    @Synchronized
    private fun getOrAssignTransactionId(remoteId: String): Long {
        return transactionIdMap.getOrPut(remoteId) {
            nextTransactionId++
        }
    }

    @Synchronized
    private fun getOrAssignNotificationId(remoteId: String): Long {
        return notificationIdMap.getOrPut(remoteId) {
            nextNotificationId++
        }
    }

    fun startPolling() {
        if (pollingJob?.isActive == true) return
        pollingJob = viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                if (FirebaseAuth.getInstance().currentUser != null) {
                    syncAll()
                }
                delay(3_000)
            }
        }
    }

    fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    suspend fun syncAll() {
        runCatching { refreshBalances() }
        runCatching { refreshTransactions() }
        runCatching { refreshNotifications() }
    }

    override fun onCleared() {
        super.onCleared()
        stopPolling()
    }

    init {
        // Seed the feed with the recent payments people sent this user.
        transactions.filter { it.direction == TransactionDirection.Incoming }.forEach { tx ->
            val asset = Asset.fromCode(tx.currency) ?: Asset.LKR
            account.notify(
                NotificationKind.Transaction,
                "Payment received",
                "${tx.counterpartyName} sent you ${asset.code} ${tx.amount.toAmountString(asset)}.",
                transactionId = tx.id,
                timestamp = tx.timestamp,
                read = now - tx.timestamp > 60 * 60_000,
            )
        }
    }

    /** Distinct people from recent transactions, newest first, for quick re-sends. */
    val recentRecipients: List<PayCeylonUser>
        get() = transactions
            .filter { it.direction == TransactionDirection.Outgoing || it.direction == TransactionDirection.Incoming }
            .map { tx ->
                findPayCeylonUser(tx.counterpartyHandle)
                    ?: PayCeylonUser(tx.counterpartyName, tx.counterpartyHandle)
            }
            .filter { it.handle.isNotBlank() }
            .distinctBy { it.handle.lowercase() }

    fun balanceOf(asset: Asset): BigDecimal = balances.value.getValue(asset)

    suspend fun refreshBalances() {
        val response = BackendApi.get("/wallet/balances").getJSONObject("balances")
        val updated = balances.value.toMutableMap()
        Asset.entries.forEach { asset ->
            if (response.has(asset.code)) {
                updated[asset] = response.getString(asset.code).toBigDecimal()
            }
        }
        balances.value = updated
    }

    suspend fun refreshTransactions() {
        val response = BackendApi.get("/transactions?limit=50")
        val items = response.getJSONArray("items")
        val remoteTxs = mutableListOf<WalletTransaction>()
        for (i in 0 until items.length()) {
            val item = items.getJSONObject(i)
            val remoteId = item.optString("id")
            val direction = when (item.optString("direction").uppercase()) {
                "INCOMING" -> TransactionDirection.Incoming
                "OUTGOING" -> TransactionDirection.Outgoing
                "SWAP" -> TransactionDirection.Swap
                "TOP_UP" -> TransactionDirection.TopUp
                "WITHDRAWAL" -> TransactionDirection.Withdrawal
                else -> TransactionDirection.Incoming
            }
            val wallet = if (item.optString("wallet").uppercase() == "CRYPTO") {
                WalletType.Crypto
            } else {
                WalletType.Fiat
            }
            val cpObj = item.optJSONObject("counterparty")
            val cpName = cpObj?.optString("name")?.takeUnless { it.isNullOrBlank() } ?: "PayCeylon User"
            val cpHandle = cpObj?.optString("handle")?.takeUnless { it.isNullOrBlank() } ?: ""
            val currency = item.optString("currency", "LKR")
            val amount = item.optString("amount", "0").toBigDecimalOrNull() ?: BigDecimal.ZERO
            val timestamp = item.optLong("timestamp", System.currentTimeMillis())
            val toCurrency = item.optString("toCurrency").takeUnless { it.isNullOrBlank() || it == "null" }
            val toAmount = item.optString("toAmount").takeUnless { it.isNullOrBlank() || it == "null" }?.toBigDecimalOrNull()
            val fee = item.optString("fee").takeUnless { it.isNullOrBlank() || it == "null" }?.toBigDecimalOrNull()
            val feeCurrency = item.optString("feeCurrency").takeUnless { it.isNullOrBlank() || it == "null" }
            val localId = getOrAssignTransactionId(remoteId)
            remoteTxs.add(
                WalletTransaction(
                    id = localId,
                    counterpartyName = cpName,
                    counterpartyHandle = cpHandle,
                    direction = direction,
                    currency = currency,
                    amount = amount,
                    wallet = wallet,
                    timestamp = timestamp,
                    toCurrency = toCurrency,
                    toAmount = toAmount,
                    fee = fee,
                    feeCurrency = feeCurrency,
                    remoteId = remoteId,
                )
            )
        }

        for (tx in remoteTxs) {
            if (tx.direction == TransactionDirection.Incoming && tx.remoteId != null) {
                if (!hasSyncedTransactions) {
                    notifiedIncomingIds.add(tx.remoteId)
                } else if (tx.remoteId !in notifiedIncomingIds) {
                    notifiedIncomingIds.add(tx.remoteId)
                    val asset = Asset.fromCode(tx.currency) ?: Asset.LKR
                    _incomingPaymentEvent.tryEmit(
                        "Payment received: ${tx.counterpartyName} sent you ${tx.currency} ${tx.amount.toAmountString(asset)}"
                    )
                }
            }
        }
        hasSyncedTransactions = true

        if (transactions.toList() != remoteTxs) {
            transactions.clear()
            transactions.addAll(remoteTxs)
        }
    }

    suspend fun refreshNotifications() {
        val response = BackendApi.get("/notifications?limit=50")
        val items = response.getJSONArray("items")
        val remoteNotifs = mutableListOf<AppNotification>()
        for (i in 0 until items.length()) {
            val item = items.getJSONObject(i)
            val remoteId = item.optString("id")
            val kind = when (item.optString("kind").uppercase()) {
                "TRANSACTION" -> NotificationKind.Transaction
                "SECURITY" -> NotificationKind.Security
                else -> NotificationKind.Account
            }
            val title = item.optString("title", "Notification")
            val body = item.optString("body", "")
            val timestamp = item.optLong("timestamp", System.currentTimeMillis())
            val read = item.optBoolean("read", false)
            val txRemoteId = item.optString("transactionId").takeUnless { it.isNullOrBlank() || it == "null" }
            val txLocalId = txRemoteId?.let { getOrAssignTransactionId(it) }
            val localId = getOrAssignNotificationId(remoteId)
            remoteNotifs.add(
                AppNotification(
                    id = localId,
                    kind = kind,
                    title = title,
                    body = body,
                    timestamp = timestamp,
                    transactionId = txLocalId,
                    read = read,
                    remoteId = remoteId,
                )
            )
        }
        hasSyncedNotifications = true
        account.replaceNotifications(remoteNotifs)
    }

    /**
     * Percentage change in [asset]'s balance compared with 30 days ago, worked back from the
     * transactions since then. Null when there was no balance a month ago to compare with.
     */
    fun monthlyChangePercent(asset: Asset, now: Long = System.currentTimeMillis()): BigDecimal? {
        val since = now - 30L * 86_400_000
        val net = transactions.filter { it.timestamp >= since }
            .fold(BigDecimal.ZERO) { total, tx -> total + tx.netChange(asset) }
        val current = balanceOf(asset)
        val monthAgo = current - net
        if (monthAgo <= BigDecimal.ZERO) return null
        return (current - monthAgo).multiply(BigDecimal(100)).divide(monthAgo, 1, RoundingMode.HALF_UP)
    }

    /**
     * Debits [asset] (plus the network fee for tokens) and records the transfer. Returns false
     * when the amount and fee together exceed the balance.
     */
    suspend fun send(handle: String, amount: BigDecimal, asset: Asset): Boolean {
        val balance = balanceOf(asset)
        val fee = WalletFees.networkFee(asset) ?: BigDecimal.ZERO
        if (amount <= BigDecimal.ZERO || amount + fee > balance) return false
        val response = BackendApi.post(
            "/payments",
            JSONObject()
                .put("to", handle.removePrefix("@"))
                .put("amount", amount.toPlainString())
                .put("currency", asset.code),
            idempotencyKey = UUID.randomUUID().toString(),
        )
        val refreshed = runCatching { refreshBalances() }.isSuccess
        if (!refreshed) balances.value = balances.value + (asset to balance - amount - fee)
        val remoteId = response.optString("id").takeIf(String::isNotBlank)
        val localId = remoteId?.let { getOrAssignTransactionId(it) } ?: ((transactions.maxOfOrNull { it.id } ?: 0) + 1)
        val tx = WalletTransaction(
            id = localId,
            counterpartyName = findPayCeylonUser(handle)?.name?.substringBefore(' ')
                ?: handle.replaceFirstChar { it.uppercase() },
            counterpartyHandle = handle,
            direction = TransactionDirection.Outgoing,
            currency = asset.code,
            amount = amount,
            wallet = asset.wallet,
            timestamp = System.currentTimeMillis(),
            fee = fee.takeIf { it.signum() > 0 },
            feeCurrency = asset.code.takeIf { fee.signum() > 0 },
            remoteId = remoteId,
        )
        transactions.add(0, tx)
        notifyTransaction(tx, "Payment sent", "${asset.code} ${amount.toAmountString(asset)} to @$handle.")
        return true
    }

    /**
     * Swaps [amount] of [from] into [to] at [SwapRates]. Returns what was received, or null
     * when the amount is invalid or exceeds the balance.
     */
    suspend fun swap(from: Asset, to: Asset, amount: BigDecimal): BigDecimal? {
        val balance = balanceOf(from)
        if (from == to || amount <= BigDecimal.ZERO || amount > balance) return null
        val quote = BackendApi.post(
            "/swaps/quote",
            JSONObject().put("from", from.code).put("to", to.code).put("amount", amount.toPlainString()),
        )
        val response = BackendApi.post(
            "/swaps",
            JSONObject().put("quoteId", quote.getString("quoteId")),
            idempotencyKey = UUID.randomUUID().toString(),
        )
        val received = response.optString("toAmount").toBigDecimalOrNull() ?: SwapRates.quote(from, to, amount)
        if (received <= BigDecimal.ZERO) return null
        val refreshed = runCatching { refreshBalances() }.isSuccess
        if (!refreshed) {
            balances.value = balances.value +
                (from to balance - amount) +
                (to to balanceOf(to) + received)
        }
        val tx = WalletTransaction(
            id = (transactions.maxOfOrNull { it.id } ?: 0) + 1,
            counterpartyName = "PayCeylon Swap",
            counterpartyHandle = "payceylon",
            direction = TransactionDirection.Swap,
            currency = from.code,
            amount = amount,
            wallet = if (to.wallet == WalletType.Crypto) WalletType.Crypto else from.wallet,
            timestamp = System.currentTimeMillis(),
            toCurrency = to.code,
            toAmount = received,
            remoteId = response.optString("id").takeIf(String::isNotBlank),
        )
        transactions.add(0, tx)
        notifyTransaction(
            tx,
            "Swap complete",
            "${from.code} ${amount.toAmountString(from)} → ${to.code} ${received.toAmountString(to)}.",
        )
        return received
    }

    /**
     * Adds [amount] LKR to the fiat wallet from a linked card or bank. Crypto can't be topped
     * up directly; it is bought by swapping LKR. Returns false over the per-top-up limit.
     */
    suspend fun topUp(amount: BigDecimal, method: PaymentMethod): Boolean {
        if (amount <= BigDecimal.ZERO || amount > WalletFees.maxTopUpLkr) return false
        val response = BackendApi.post(
            "/topups",
            JSONObject()
                .put("amount", amount.toPlainString())
                .put("paymentMethodId", resolvePaymentMethodId(method)),
            idempotencyKey = UUID.randomUUID().toString(),
        )
        val previousBalance = balanceOf(Asset.LKR)
        val refreshed = runCatching { refreshBalances() }.isSuccess
        if (!refreshed) balances.value = balances.value + (Asset.LKR to previousBalance + amount)
        val tx = recordExternal(
            TransactionDirection.TopUp,
            method,
            amount,
            WalletFees.topUpFee(amount, method),
            response.optString("id").takeIf(String::isNotBlank),
        )
        notifyTransaction(tx, "Top-up successful", "LKR ${amount.toMoneyString()} added from ${method.title}.")
        return true
    }

    /**
     * Pays [amount] LKR out to a linked bank account, plus [WalletFees.withdrawalFee]. Crypto
     * must be swapped to LKR first. Returns false when amount and fee exceed the balance.
     */
    suspend fun withdraw(amount: BigDecimal, bank: PaymentMethod): Boolean {
        val fee = WalletFees.withdrawalFee
        val balance = balanceOf(Asset.LKR)
        if (bank.type != PaymentMethodType.Bank || amount <= BigDecimal.ZERO || amount + fee > balance) return false
        val response = BackendApi.post(
            "/withdrawals",
            JSONObject()
                .put("amount", amount.toPlainString())
                .put("paymentMethodId", resolvePaymentMethodId(bank)),
            idempotencyKey = UUID.randomUUID().toString(),
        )
        val refreshed = runCatching { refreshBalances() }.isSuccess
        if (!refreshed) balances.value = balances.value + (Asset.LKR to balance - amount - fee)
        val tx = recordExternal(
            TransactionDirection.Withdrawal,
            bank,
            amount,
            fee,
            response.optString("id").takeIf(String::isNotBlank),
        )
        notifyTransaction(tx, "Withdrawal on its way", "LKR ${amount.toMoneyString()} to ${bank.title}. ETA: ${bank.eta.lowercase()}.")
        return true
    }

    private fun notifyTransaction(tx: WalletTransaction, title: String, body: String) {
        account.notify(NotificationKind.Transaction, title, body, transactionId = tx.id, timestamp = tx.timestamp)
    }

    private fun recordExternal(
        direction: TransactionDirection,
        method: PaymentMethod,
        amount: BigDecimal,
        fee: BigDecimal,
        remoteId: String? = null,
    ): WalletTransaction {
        val tx = WalletTransaction(
            id = (transactions.maxOfOrNull { it.id } ?: 0) + 1,
            counterpartyName = method.name,
            counterpartyHandle = method.masked,
            direction = direction,
            currency = Asset.LKR.code,
            amount = amount,
            wallet = WalletType.Fiat,
            timestamp = System.currentTimeMillis(),
            fee = fee,
            feeCurrency = Asset.LKR.code,
            remoteId = remoteId,
        )
        transactions.add(0, tx)
        return tx
    }

    private suspend fun resolvePaymentMethodId(method: PaymentMethod): String {
        method.remoteId?.let { return it }
        val items = BackendApi.get("/payment-methods").getJSONArray("items")
        for (index in 0 until items.length()) {
            val item = items.getJSONObject(index)
            if (
                item.optString("type").equals(method.type.name, ignoreCase = true) &&
                item.optString("name").equals(method.name, ignoreCase = true) &&
                item.optString("last4") == method.last4
            ) {
                return item.getString("id")
            }
        }
        error("This payment method is no longer linked. Refresh your payment methods and try again.")
    }
}

private val moneyFormat = DecimalFormat("#,##0.00", DecimalFormatSymbols(Locale.US))

/** Splits an amount into its grouped whole part ("32,483") and decimal part (".75"). */
internal fun BigDecimal.toMoneyParts(decimals: Int = 2): Pair<String, String> {
    if (decimals == 0) {
        return DecimalFormat("#,##0", DecimalFormatSymbols(Locale.US)).format(setScale(0, RoundingMode.DOWN)) to ""
    }
    val format = if (decimals == 2) {
        moneyFormat
    } else {
        DecimalFormat("#,##0." + "0".repeat(decimals), DecimalFormatSymbols(Locale.US))
    }
    val formatted = format.format(setScale(decimals, RoundingMode.DOWN))
    val dot = formatted.lastIndexOf('.')
    return formatted.substring(0, dot) to formatted.substring(dot)
}

internal fun BigDecimal.toMoneyString(): String = moneyFormat.format(setScale(2, RoundingMode.DOWN))

/**
 * Formats an amount for its currency: fiat and USDT always show 2 decimals; other tokens
 * show up to their own precision with trailing zeros trimmed (0.0125 BTC, not 0.01).
 */
internal fun BigDecimal.toAmountParts(currency: String): Pair<String, String> {
    val asset = Asset.fromCode(currency)
    if (asset == null || asset.decimals <= 2) return toMoneyParts()
    val (whole, fraction) = toMoneyParts(asset.decimals)
    return whole to fraction.trimEnd('0').padEnd(3, '0')
}

internal fun BigDecimal.toAmountString(asset: Asset): String =
    toAmountParts(asset.code).let { (whole, fraction) -> whole + fraction }
