package com.pocketbank.app

import android.content.Context
import com.getcapacitor.JSObject
import com.getcapacitor.Plugin
import com.getcapacitor.PluginCall
import com.getcapacitor.PluginMethod
import com.getcapacitor.annotation.CapacitorPlugin

@CapacitorPlugin(name = "SharedPrefs")
class SharedPrefsPlugin : Plugin() {

    @PluginMethod
    fun set(call: PluginCall) {
        val key = call.getString("key") ?: return call.reject("key required")
        val value = call.getString("value") ?: return call.reject("value required")
        context.getSharedPreferences("PocketBankWidget", Context.MODE_PRIVATE)
            .edit().putString(key, value).apply()
        call.resolve()
    }

    @PluginMethod
    fun get(call: PluginCall) {
        val key = call.getString("key") ?: return call.reject("key required")
        val value = context.getSharedPreferences("PocketBankWidget", Context.MODE_PRIVATE)
            .getString(key, null)
        val ret = JSObject()
        ret.put("value", value)
        call.resolve(ret)
    }
}
