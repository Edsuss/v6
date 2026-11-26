package com.caveirax.micbooster;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

@CapacitorPlugin(name = "BgControl")
public class BgControlPlugin extends Plugin {
    private static BgControlPlugin instance;

    @Override public void load() { instance = this; }

    static void emitToggle() {
        if (instance != null) instance.notifyListeners("toggle", new JSObject(), true);
    }

    /** Pede permissão de "sobrepor outros apps" e de notificações. */
    @PluginMethod
    public void prepare(PluginCall call) {
        Activity a = getActivity();
        boolean overlay = Build.VERSION.SDK_INT < 23 || Settings.canDrawOverlays(a);
        if (Build.VERSION.SDK_INT >= 33) {
            a.requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 9001);
        }
        if (!overlay) {
            Intent i = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + a.getPackageName()));
            a.startActivity(i);
        }
        JSObject r = new JSObject();
        r.put("overlay", overlay);
        call.resolve(r);
    }

    /** running = serviço de segundo plano ligado; active = bolinha verde/cinza. */
    @PluginMethod
    public void setState(PluginCall call) {
        boolean running = Boolean.TRUE.equals(call.getBoolean("running", false));
        boolean active = Boolean.TRUE.equals(call.getBoolean("active", false));
        Activity a = getActivity();
        Intent svc = new Intent(a, MicForegroundService.class);
        try {
            if (running) {
                if (Build.VERSION.SDK_INT >= 26) a.startForegroundService(svc); else a.startService(svc);
            } else {
                a.stopService(svc);
            }
        } catch (Exception e) { /* ignora */ }
        BubbleService.setActive(active);
        call.resolve();
    }

    /** Traz o app para frente (usado quando a bolinha é tocada com o app parado). */
    @PluginMethod
    public void bringToFront(PluginCall call) {
        Activity a = getActivity();
        Intent i = new Intent(a, MainActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        a.startActivity(i);
        call.resolve();
    }
}
