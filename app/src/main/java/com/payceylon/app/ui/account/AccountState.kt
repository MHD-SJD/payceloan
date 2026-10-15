package com.payceylon.app.ui.account

<<<<<<< HEAD
import android.graphics.Bitmap
import android.graphics.BitmapFactory
=======
>>>>>>> dd9f1d175672de1daa0c6fec1db2025dcf550834
import androidx.annotation.DrawableRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
<<<<<<< HEAD
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.payceylon.app.R
import com.payceylon.app.data.ApiException
import com.payceylon.app.data.Backend
import com.payceylon.app.data.local.WalletSnapshot
import com.payceylon.app.ui.auth.formatSriLankanMobileNumber
import com.payceylon.app.ui.wallet.PayCeylonUser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.math.BigDecimal
import java.util.UUID
=======
import com.payceylon.app.R
import com.payceylon.app.ui.auth.formatSriLankanMobileNumber
import com.payceylon.app.ui.wallet.CurrentUser
import com.payceylon.app.ui.wallet.PayCeylonUser
import java.math.BigDecimal
>>>>>>> dd9f1d175672de1daa0c6fec1db2025dcf550834

enum class PaymentMethodType { Card, Bank }

/**
 * A linked card or bank account. Only the last four digits are kept: full card numbers and
 * CVVs never leave the add-card form (a real build hands them to a PCI-compliant tokenizer).
 */
data class PaymentMethod(
<<<<<<< HEAD
    val id: String,
=======
    val id: Long,
>>>>>>> dd9f1d175672de1daa0c6fec1db2025dcf550834
    val type: PaymentMethodType,
    /** Card brand ("Visa") or bank name ("Commercial Bank"). */
    val name: String,
    val last4: String,
    val holder: String,
    /** "Expires 08/29" or "Savings account". */
    val detail: String,
    /** Cards only. */
    val brand: CardBrand? = null,
<<<<<<< HEAD
=======
    /** Backend UUID when this method has been synced. */
    val remoteId: String? = null,
>>>>>>> dd9f1d175672de1daa0c6fec1db2025dcf550834
) {
    val masked: String get() = "•••• $last4"
    val title: String get() = "$name $masked"

    @get:DrawableRes
    val icon: Int get() = if (type == PaymentMethodType.Card) R.drawable.ic_top_up else R.drawable.ic_withdraw

    /** Charged on top of a top-up from this method. */
    val topUpFeeRate: BigDecimal get() = if (type == PaymentMethodType.Card) BigDecimal("0.015") else BigDecimal.ZERO

    /** How long money takes to arrive from (top-up) or at (withdrawal) this method. */
<<<<<<< HEAD
    val eta: String get() = if (type == PaymentMethodType.Card) "Instant" else "About 5 minutes"
=======
    val eta: String get() = if (type == PaymentMethodType.Card) "Instant" else "Within 1 hour"
>>>>>>> dd9f1d175672de1daa0c6fec1db2025dcf550834
}

enum class CardBrand(
    val label: String,
    @param:DrawableRes val logo: Int,
    /** Valid card-number lengths. */
    val lengths: IntRange,
    val cvvLength: Int,
) {
    Visa("Visa", R.drawable.ic_visa, 16..16, 3),
    Mastercard("Mastercard", R.drawable.ic_brand_mastercard, 16..16, 3),
    Amex("AMEX", R.drawable.ic_brand_amex, 15..15, 4),
    UnionPay("UnionPay", R.drawable.ic_brand_unionpay, 16..19, 3),
    Jcb("JCB", R.drawable.ic_brand_jcb, 16..16, 3),
    Discover("Discover", R.drawable.ic_brand_discover, 16..16, 3),
    Diners("Diners", R.drawable.ic_brand_diners, 14..14, 3),
    ;

    companion object {
        /** Brand from the card number's leading digits (IIN range), or null if unknown. */
        fun detect(number: String): CardBrand? {
            val digits = number.filter(Char::isDigit)
            fun prefix(length: Int) = if (digits.length >= length) digits.take(length).toInt() else -1
            val two = prefix(2)
            val three = prefix(3)
            val four = prefix(4)
            return when {
                digits.startsWith("4") -> Visa
                two in 51..55 || four in 2221..2720 -> Mastercard
                two == 34 || two == 37 -> Amex
                two == 62 -> UnionPay
                four in 3528..3589 -> Jcb
                four == 6011 || two == 65 || three in 644..649 -> Discover
                two == 36 || two == 38 || two == 39 || three in 300..305 -> Diners
                else -> null
            }
        }
    }
}

/** Licensed commercial banks offered when linking an account. */
val SriLankanBanks = listOf(
    "Bank of Ceylon",
    "People's Bank",
    "Commercial Bank",
    "Hatton National Bank",
    "Sampath Bank",
    "Seylan Bank",
    "NDB Bank",
    "Nations Trust Bank",
    "DFCC Bank",
    "Pan Asia Bank",
)

enum class NotificationKind { Transaction, Account, Security }

data class AppNotification(
<<<<<<< HEAD
    val id: String,
=======
    val id: Long,
>>>>>>> dd9f1d175672de1daa0c6fec1db2025dcf550834
    val kind: NotificationKind,
    val title: String,
    val body: String,
    val timestamp: Long,
    /** Transaction notifications open that transaction. */
<<<<<<< HEAD
    val transactionId: String? = null,
    val read: Boolean = false,
=======
    val transactionId: Long? = null,
    val read: Boolean = false,
    /** Backend UUID when this notification came from the API. */
    val remoteId: String? = null,
>>>>>>> dd9f1d175672de1daa0c6fec1db2025dcf550834
)

enum class ProfileField(val label: String) {
    Name("Full name"),
    Username("Username"),
    Phone("Phone number"),
    Email("Email"),
}

data class UserProfile(
    val name: String,
    val handle: String,
    /** Nine-digit Sri Lankan mobile number without the +94 prefix. */
    val phone: String,
    val email: String,
<<<<<<< HEAD
    /** Where the profile photo is served, or null for the default avatar. */
    val photoUrl: String? = null,
=======
>>>>>>> dd9f1d175672de1daa0c6fec1db2025dcf550834
) {
    val user: PayCeylonUser get() = PayCeylonUser(name, handle)
    val displayPhone: String get() = "+94 ${formatSriLankanMobileNumber(phone)}"

    fun valueOf(field: ProfileField): String = when (field) {
        ProfileField.Name -> name
        ProfileField.Username -> handle
        ProfileField.Phone -> phone
        ProfileField.Email -> email
    }
}

/** On/off preferences on the Settings screen. */
enum class AccountSetting(
    val title: String,
    val description: String,
    @param:DrawableRes val icon: Int,
    val default: Boolean,
) {
<<<<<<< HEAD
    // Descriptions stay short enough for one line on a phone.
    BiometricApproval("Biometric approval", "Confirm payments with biometrics", R.drawable.ic_shield, true),
    HideBalance("Hide balance", "Hidden when the app opens", R.drawable.ic_eye_off, false),
    TransactionAlerts("Transaction alerts", "When money moves", R.drawable.ic_bell, true),
    Promotions("Offers & promotions", "Cashback and rewards", R.drawable.ic_gift, false),
}

/**
 * The signed-in user's profile, linked accounts, settings and notification feed, loaded from
 * the PayCeylon API. Suspend functions throw [ApiException]; quick toggles save in the
 * background on [scope] and [report] what fails.
 */
class AccountState(
    private val backend: Backend,
    private val scope: CoroutineScope,
    private val report: (String) -> Unit,
) {
    var profile by mutableStateOf(UserProfile(name = "", handle = "", phone = "", email = ""))
=======
    BiometricApproval("Biometric approval", "Confirm payments with your fingerprint", R.drawable.ic_fingerprint, true),
    HideBalance("Hide balance", "Keep your balance hidden when the app opens", R.drawable.ic_eye_off, false),
    TransactionAlerts("Transaction alerts", "Notify me when money moves", R.drawable.ic_bell, true),
    Promotions("Offers & promotions", "Cashback, rewards and PayCeylon news", R.drawable.ic_gift, false),
}

/**
 * The signed-in user's profile, linked accounts, settings and notification feed. In-memory
 * for the prototype; a real build backs this with the account API and Room.
 */
class AccountState(now: Long = System.currentTimeMillis()) {
    var profile by mutableStateOf(
        UserProfile(CurrentUser.name, CurrentUser.handle, phone = "771234567", email = "kasun.perera@gmail.com"),
    )
>>>>>>> dd9f1d175672de1daa0c6fec1db2025dcf550834
        private set

    /** Custom profile photo; null shows the default avatar. */
    var photo by mutableStateOf<ImageBitmap?>(null)
        private set

<<<<<<< HEAD
    /** The encoded image behind [photo], kept so the local database can store it for offline use. */
    var photoBytes: ByteArray? = null
        private set

    val paymentMethods = mutableStateListOf<PaymentMethod>()

    val cards: List<PaymentMethod> get() = paymentMethods.filter { it.type == PaymentMethodType.Card }

    private var primaryCardId by mutableStateOf<String?>(null)
=======
    val paymentMethods = mutableStateListOf(
        PaymentMethod(1, PaymentMethodType.Card, "Visa", "0928", "Kasun Perera", "Expires 08/29", CardBrand.Visa),
        PaymentMethod(2, PaymentMethodType.Bank, "Commercial Bank", "2207", "Kasun Perera", "Savings account"),
        PaymentMethod(3, PaymentMethodType.Bank, "Sampath Bank", "9134", "Kasun Perera", "Current account"),
    )

    val cards: List<PaymentMethod> get() = paymentMethods.filter { it.type == PaymentMethodType.Card }

    private var primaryCardId by mutableStateOf<Long?>(1)
>>>>>>> dd9f1d175672de1daa0c6fec1db2025dcf550834

    /** The card shown on the fiat balance card; falls back to the first if it was removed. */
    val primaryCard: PaymentMethod? get() = cards.firstOrNull { it.id == primaryCardId } ?: cards.firstOrNull()

<<<<<<< HEAD
    fun selectPrimaryCard(id: String) {
=======
    fun selectPrimaryCard(id: Long) {
>>>>>>> dd9f1d175672de1daa0c6fec1db2025dcf550834
        primaryCardId = id
    }
    val banks: List<PaymentMethod> get() = paymentMethods.filter { it.type == PaymentMethodType.Bank }

    private val settings = mutableStateMapOf<AccountSetting, Boolean>()

    fun isEnabled(setting: AccountSetting): Boolean = settings[setting] ?: setting.default

<<<<<<< HEAD
    /** Newest first. */
    val notifications = mutableStateListOf<AppNotification>()

    val unreadCount: Int get() = notifications.count { !it.read }

    /** Loads everything for a freshly verified [profile]. */
    suspend fun load(profile: UserProfile) = coroutineScope {
        val methods = async { backend.paymentMethods() }
        val latestSettings = async { backend.settings() }
        val feed = async { backend.notifications() }
        applyProfile(profile)
        paymentMethods.replaceWith(methods.await())
        settings.clear()
        settings.putAll(latestSettings.await())
        notifications.replaceWith(feed.await())
    }

    /** Shows a saved [snapshot] from the local database straight away, before the server answers. */
    fun restore(snapshot: WalletSnapshot) {
        profile = snapshot.profile
        photoBytes = snapshot.photo
        photo = snapshot.photo?.let(::decodePhoto)
        paymentMethods.replaceWith(snapshot.paymentMethods)
        settings.clear()
        settings.putAll(snapshot.settings)
        notifications.replaceWith(snapshot.notifications)
    }

    fun clear() {
        profile = UserProfile(name = "", handle = "", phone = "", email = "")
        photo = null
        photoBytes = null
        paymentMethods.clear()
        primaryCardId = null
        settings.clear()
        notifications.clear()
    }

    suspend fun refreshNotifications() {
        notifications.replaceWith(backend.notifications())
    }

    private suspend fun refreshNotificationsQuietly() {
        try {
            refreshNotifications()
        } catch (_: ApiException) {
        }
    }

    private suspend fun applyProfile(latest: UserProfile) {
        val photoChanged = latest.photoUrl != profile.photoUrl
        profile = latest
        if (photoChanged) {
            photoBytes = latest.photoUrl?.let { url ->
                try {
                    backend.downloadPhoto(url)
                } catch (_: ApiException) {
                    null
                }
            }
            photo = photoBytes?.let(::decodePhoto)
        }
    }

    fun markAllRead() {
        if (notifications.none { !it.read }) return
        notifications.indices.forEach { i -> if (!notifications[i].read) notifications[i] = notifications[i].copy(read = true) }
        scope.launch {
            try {
                backend.markNotificationsRead(emptyList(), all = true)
            } catch (e: ApiException) {
                report(e.message)
            }
        }
    }

    fun markRead(id: String) {
        val index = notifications.indexOfFirst { it.id == id }
        if (index < 0 || notifications[index].read) return
        notifications[index] = notifications[index].copy(read = true)
        scope.launch {
            try {
                backend.markNotificationsRead(listOf(id))
            } catch (e: ApiException) {
                report(e.message)
            }
        }
    }

    /**
     * Saves a profile field after [validateProfileField] passes. Returns why it couldn't be
     * saved (validation or server error), or null. Phone numbers go through [savePhoneChange].
     */
    suspend fun updateProfile(field: ProfileField, rawValue: String): String? {
        validateProfileField(field, rawValue)?.let { return it }
        val value = normalizeProfileValue(field, rawValue)
        if (value == profile.valueOf(field)) return null
        return save(mapOf(field to value.ifEmpty { null }))
    }

    /** Sets up a new account in one go: name and username are required, email is optional. */
    suspend fun setUpProfile(name: String, handle: String, email: String = ""): String? {
        validateProfileField(ProfileField.Name, name)?.let { return it }
        validateProfileField(ProfileField.Username, handle)?.let { return it }
        if (email.isNotBlank()) validateProfileField(ProfileField.Email, email)?.let { return it }
        return save(
            buildMap {
                put(ProfileField.Name, normalizeProfileValue(ProfileField.Name, name))
                put(ProfileField.Username, normalizeProfileValue(ProfileField.Username, handle))
                if (email.isNotBlank()) put(ProfileField.Email, normalizeProfileValue(ProfileField.Email, email))
            },
        )
    }

    private suspend fun save(changes: Map<ProfileField, String?>): String? = try {
        applyProfile(backend.updateProfile(changes))
        refreshNotificationsQuietly()
        null
    } catch (e: ApiException) {
        e.message
    }

    /** Saves the number the Firebase user was just moved to. */
    suspend fun savePhoneChange() {
        applyProfile(backend.updatePhone())
        refreshNotificationsQuietly()
    }

    suspend fun isHandleAvailable(handle: String): Boolean =
        normalizeProfileValue(ProfileField.Username, handle) == profile.handle || backend.isHandleAvailable(handle)

    /** Uploads [bitmap] as the profile photo, or removes it when null. */
    suspend fun updatePhoto(bitmap: ImageBitmap?) {
        val bytes = bitmap?.let(::encodePhoto)
        val latest = if (bytes == null) backend.removePhoto() else backend.uploadPhoto(bytes)
        // Show the picked image straight away instead of downloading it back.
        profile = latest
        photo = bitmap
        photoBytes = bytes
        refreshNotificationsQuietly()
=======
    val notifications = mutableStateListOf(
        AppNotification(1, NotificationKind.Security, "New sign-in", "You signed in on this device. Not you? Contact support.", now - 60_000, read = false),
        AppNotification(2, NotificationKind.Account, "Welcome to PayCeylon", "Your Fiat and Crypto wallets are ready.", now - 3 * 86_400_000L, read = true),
    )

    val unreadCount: Int get() = notifications.count { !it.read }

    fun notify(kind: NotificationKind, title: String, body: String, transactionId: Long? = null, timestamp: Long = System.currentTimeMillis(), read: Boolean = false) {
        if (kind == NotificationKind.Transaction && !isEnabled(AccountSetting.TransactionAlerts)) return
        val notification = AppNotification(
            id = (notifications.maxOfOrNull { it.id } ?: 0) + 1,
            kind = kind,
            title = title,
            body = body,
            timestamp = timestamp,
            transactionId = transactionId,
            read = read,
        )
        // Newest first.
        val index = notifications.indexOfFirst { it.timestamp < timestamp }.let { if (it < 0) notifications.size else it }
        notifications.add(index, notification)
    }

    fun markAllRead() {
        notifications.indices.forEach { i -> if (!notifications[i].read) notifications[i] = notifications[i].copy(read = true) }
    }

    fun markRead(id: Long) {
        val index = notifications.indexOfFirst { it.id == id }
        if (index >= 0) notifications[index] = notifications[index].copy(read = true)
    }

    fun replaceNotifications(newNotifications: List<AppNotification>) {
        if (notifications.toList() != newNotifications) {
            notifications.clear()
            notifications.addAll(newNotifications)
        }
    }

    fun replacePaymentMethods(methods: List<PaymentMethod>) {
        paymentMethods.clear()
        paymentMethods.addAll(methods)
        if (cards.none { it.id == primaryCardId }) primaryCardId = cards.firstOrNull()?.id
    }

    fun attachRemotePaymentMethod(localId: Long, remoteId: String) {
        val index = paymentMethods.indexOfFirst { it.id == localId }
        if (index >= 0) paymentMethods[index] = paymentMethods[index].copy(remoteId = remoteId)
    }

    fun restoreSettings(values: Map<AccountSetting, Boolean>) {
        settings.clear()
        settings.putAll(values)
    }

    /**
     * Saves a profile field after [validateProfileField] passes. Returns the validation error
     * instead when it doesn't. Phone numbers must already be verified by SMS code.
     */
    fun updateProfile(field: ProfileField, rawValue: String): String? {
        validateProfileField(field, rawValue, profile.handle)?.let { return it }
        val value = normalizeProfileValue(field, rawValue)
        if (value == profile.valueOf(field)) return null
        profile = when (field) {
            ProfileField.Name -> profile.copy(name = value)
            ProfileField.Username -> profile.copy(handle = value)
            ProfileField.Phone -> profile.copy(phone = value)
            ProfileField.Email -> profile.copy(email = value)
        }
        val (kind, body) = when (field) {
            ProfileField.Name -> NotificationKind.Account to "Your name is now ${profile.name}."
            ProfileField.Username -> NotificationKind.Account to "Your username is now @${profile.handle}. Old QR codes no longer work."
            ProfileField.Phone -> NotificationKind.Security to "Your phone number was changed to ${profile.displayPhone}."
            ProfileField.Email -> NotificationKind.Security to "Your email was changed to ${profile.email}."
        }
        notify(kind, "${field.label} updated", body)
        return null
    }

    /** Records the number the user just verified at sign-in. */
    fun signedIn(phone: String) {
        val digits = normalizeProfileValue(ProfileField.Phone, phone)
        if (digits.length == 9) profile = profile.copy(phone = digits)
    }

    fun restoreProfile(name: String, handle: String, phone: String, email: String) {
        val digits = normalizeProfileValue(ProfileField.Phone, phone)
        profile = UserProfile(name, handle, digits, email)
    }

    fun updatePhoto(bitmap: ImageBitmap?) {
        photo = bitmap
        notify(NotificationKind.Account, if (bitmap == null) "Profile photo removed" else "Profile photo updated", "Your new look is visible to people you pay.")
>>>>>>> dd9f1d175672de1daa0c6fec1db2025dcf550834
    }

    fun setEnabled(setting: AccountSetting, enabled: Boolean) {
        if (isEnabled(setting) == enabled) return
        settings[setting] = enabled
<<<<<<< HEAD
        scope.launch {
            try {
                settings.putAll(backend.updateSetting(setting, enabled))
            } catch (e: ApiException) {
                settings[setting] = !enabled
                report(e.message)
            }
        }
    }

    /**
     * Links a card. The number and CVV never leave the device: like a payment provider's SDK,
     * this keeps only brand, last four and expiry and sends a demo token in their place.
     */
    suspend fun addCard(number: String, expiry: String, holder: String): PaymentMethod {
        val digits = number.filter(Char::isDigit)
        val method = backend.addCard(
            token = "demo_tok_" + UUID.randomUUID().toString().replace("-", ""),
            holder = holder.trim(),
            brand = CardBrand.detect(digits)?.label ?: "Card",
            last4 = digits.takeLast(4),
            expiry = "${expiry.take(2)}/${expiry.drop(2)}",
        )
        paymentMethods.add(paymentMethods.indexOfLast { it.type == PaymentMethodType.Card } + 1, method)
        refreshNotificationsQuietly()
        return method
    }

    suspend fun addBank(bank: String, accountNumber: String, holder: String, isSavings: Boolean): PaymentMethod {
        val method = backend.addBank(bank, accountNumber.filter(Char::isDigit), holder.trim(), isSavings)
        paymentMethods.add(method)
        refreshNotificationsQuietly()
        return method
    }

    suspend fun removePaymentMethod(id: String) {
        backend.removePaymentMethod(id)
        paymentMethods.removeAll { it.id == id }
        refreshNotificationsQuietly()
    }
}

private fun <T> MutableList<T>.replaceWith(items: List<T>) {
    if (this == items) return
    clear()
    addAll(items)
}

private fun encodePhoto(bitmap: ImageBitmap): ByteArray =
    ByteArrayOutputStream().also { bitmap.asAndroidBitmap().compress(Bitmap.CompressFormat.JPEG, 88, it) }.toByteArray()

private fun decodePhoto(bytes: ByteArray): ImageBitmap? =
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
=======
        notify(
            if (setting == AccountSetting.BiometricApproval) NotificationKind.Security else NotificationKind.Account,
            "${setting.title} turned ${if (enabled) "on" else "off"}",
            setting.description + ".",
        )
    }

    /** Links a card; only the brand, last four digits and expiry are kept. */
    fun addCard(number: String, expiry: String, holder: String): PaymentMethod {
        val digits = number.filter(Char::isDigit)
        val brand = CardBrand.detect(digits)
        val method = PaymentMethod(
            id = nextMethodId(),
            type = PaymentMethodType.Card,
            name = brand?.label ?: "Card",
            last4 = digits.takeLast(4),
            holder = holder.trim(),
            detail = "Expires ${expiry.take(2)}/${expiry.drop(2)}",
            brand = brand,
        )
        paymentMethods.add(paymentMethods.indexOfLast { it.type == PaymentMethodType.Card } + 1, method)
        notify(NotificationKind.Security, "Card added", "${method.title} can now be used to top up.")
        return method
    }

    fun addBank(bank: String, accountNumber: String, holder: String, isSavings: Boolean): PaymentMethod {
        val method = PaymentMethod(
            id = nextMethodId(),
            type = PaymentMethodType.Bank,
            name = bank,
            last4 = accountNumber.filter(Char::isDigit).takeLast(4),
            holder = holder.trim(),
            detail = if (isSavings) "Savings account" else "Current account",
        )
        paymentMethods.add(method)
        notify(NotificationKind.Security, "Bank account linked", "${method.title} can now be used to top up and withdraw.")
        return method
    }

    fun removePaymentMethod(id: Long) {
        val method = paymentMethods.firstOrNull { it.id == id } ?: return
        paymentMethods.remove(method)
        notify(NotificationKind.Security, "${if (method.type == PaymentMethodType.Card) "Card" else "Bank account"} removed", "${method.title} is no longer linked.")
    }

    private fun nextMethodId(): Long = (paymentMethods.maxOfOrNull { it.id } ?: 0) + 1
}
>>>>>>> dd9f1d175672de1daa0c6fec1db2025dcf550834
