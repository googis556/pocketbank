package com.pocketbank.app;

import android.content.Context;
import android.content.SharedPreferences;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

@CapacitorPlugin(name = "SharedPrefs")
public class SharedPrefsPlugin extends Plugin {

    @PluginMethod
    public void set(PluginCall call) {
        String key = call.getString("key");
        String value = call.getString("value");
        if (key == null) { call.reject("key required"); return; }
        if (value == null) { call.reject("value required"); return; }
        getContext().getSharedPreferences("PocketBankWidget", Context.MODE_PRIVATE)
            .edit().putString(key, value).apply();
        call.resolve();
    }

    @PluginMethod
    public void get(PluginCall call) {
        String key = call.getString("key");
        if (key == null) { call.reject("key required"); return; }
        String value = getContext().getSharedPreferences("PocketBankWidget", Context.MODE_PRIVATE)
            .getString(key, null);
        JSObject ret = new JSObject();
        ret.put("value", value);
        call.resolve(ret);
    }
}
