package com.caveirax.micbooster;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.webkit.WebView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        registerPlugin(AudioRoutePlugin.class);
        registerPlugin(BgControlPlugin.class);
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= 31
                && ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 77);
        }
    }

    // Impede o WebView (JS + áudio) de congelar quando o app sai da tela
    private void keepWebViewAlive() {
        try {
            WebView w = getBridge().getWebView();
            w.onResume();
            w.resumeTimers();
        } catch (Exception ignored) {}
    }

    @Override public void onPause() {
        super.onPause();
        keepWebViewAlive();
        boolean canOverlay = Build.VERSION.SDK_INT < 23 || Settings.canDrawOverlays(this);
        if (canOverlay) startService(new Intent(this, BubbleService.class)); // mostra a bolinha
    }

    @Override public void onStop() {
        super.onStop();
        keepWebViewAlive();
    }

    @Override public void onResume() {
        super.onResume();
        stopService(new Intent(this, BubbleService.class)); // esconde a bolinha
    }
}
