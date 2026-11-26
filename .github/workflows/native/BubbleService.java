package com.caveirax.micbooster;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;

/** Bolinha flutuante: toque = ativar/desativar; arraste = mover. */
public class BubbleService extends Service {
    static boolean active = false; // verde = processando, cinza = pausado
    private static BubbleService inst;
    private WindowManager wm;
    private FrameLayout bubble;
    private GradientDrawable ring;
    private WindowManager.LayoutParams lp;

    static void setActive(boolean a) {
        active = a;
        if (inst != null && inst.ring != null) inst.paint();
    }

    private void paint() {
        ring.setStroke(dp(4), active ? Color.parseColor("#22E06B") : Color.parseColor("#8A8A99"));
    }

    private int dp(int v) { return (int) (v * getResources().getDisplayMetrics().density); }

    @Override public int onStartCommand(Intent i, int f, int id) {
        if (bubble != null) return START_NOT_STICKY;
        if (Build.VERSION.SDK_INT >= 23 && !Settings.canDrawOverlays(this)) { stopSelf(); return START_NOT_STICKY; }
        inst = this;
        wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        int size = dp(60);

        ring = new GradientDrawable();
        ring.setShape(GradientDrawable.OVAL);
        ring.setColor(Color.parseColor("#07040D"));
        paint();

        ImageView img = new ImageView(this);
        img.setImageResource(R.mipmap.ic_launcher_round);
        img.setPadding(dp(5), dp(5), dp(5), dp(5));
        bubble = new FrameLayout(this);
        bubble.setBackground(ring);
        bubble.setElevation(dp(6));
        bubble.addView(img, new FrameLayout.LayoutParams(size, size));

        int type = Build.VERSION.SDK_INT >= 26 ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;
        lp = new WindowManager.LayoutParams(size, size, type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.TOP | Gravity.START;
        lp.x = dp(12); lp.y = dp(200);

        bubble.setOnTouchListener(new View.OnTouchListener() {
            int sx, sy; float tx, ty; boolean moved;
            @Override public boolean onTouch(View v, MotionEvent e) {
                switch (e.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        sx = lp.x; sy = lp.y; tx = e.getRawX(); ty = e.getRawY(); moved = false; return true;
                    case MotionEvent.ACTION_MOVE:
                        float dx = e.getRawX() - tx, dy = e.getRawY() - ty;
                        if (Math.abs(dx) > dp(6) || Math.abs(dy) > dp(6)) moved = true;
                        if (moved) { lp.x = sx + (int) dx; lp.y = sy + (int) dy; wm.updateViewLayout(bubble, lp); }
                        return true;
                    case MotionEvent.ACTION_UP:
                        if (!moved) BgControlPlugin.emitToggle();
                        return true;
                }
                return false;
            }
        });
        wm.addView(bubble, lp);
        return START_NOT_STICKY;
    }

    @Override public void onDestroy() {
        if (bubble != null && wm != null) { try { wm.removeView(bubble); } catch (Exception ignored) {} }
        bubble = null; inst = null;
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent i) { return null; }
}
