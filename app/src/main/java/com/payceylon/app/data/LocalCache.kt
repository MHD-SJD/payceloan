package com.payceylon.app.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class LocalCache(context: Context) : SQLiteOpenHelper(context, "payceylon-cache.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE cache (key TEXT PRIMARY KEY NOT NULL, value TEXT NOT NULL)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

    fun get(key: String): String? =
        readableDatabase.rawQuery("SELECT value FROM cache WHERE key=?", arrayOf(key)).use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }

    fun put(key: String, value: String) {
        writableDatabase.execSQL(
            "INSERT OR REPLACE INTO cache(key,value) VALUES(?,?)",
            arrayOf(key, value),
        )
    }

    fun clear() {
        writableDatabase.delete("cache", null, null)
    }
}
