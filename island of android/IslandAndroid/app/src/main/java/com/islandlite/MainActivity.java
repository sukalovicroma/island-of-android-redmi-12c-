package com.islandlite;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private SharedPreferences sp;
    private TextView status;
    private Button btnStart;
    private final Handler h = new Handler(Looper.getMainLooper());

    private int dp(float v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    private TextView label(String s, float sizeSp, boolean bold, int color) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(sizeSp);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
    }

    private Button button(String s, View.OnClickListener l) {
        Button b = new Button(this);
        b.setText(s);
        b.setAllCaps(false);
        b.setOnClickListener(l);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(8);
        b.setLayoutParams(lp);
        return b;
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        sp = getSharedPreferences("island", MODE_PRIVATE);

        ScrollView sv = new ScrollView(this);
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(dp(20), dp(28), dp(20), dp(28));
        sv.addView(col);

        col.addView(label("Island для Android", 24, true, 0xFF111111));
        TextView sub = label("«Остров» вверху экрана: обложка, название и кнопки музыки. "
                + "Когда музыки нет — часы.", 14, false, 0xFF555555);
        sub.setPadding(0, dp(6), 0, dp(14));
        col.addView(sub);

        status = label("", 15, false, 0xFF111111);
        status.setPadding(0, 0, 0, dp(6));
        col.addView(status);

        col.addView(button("1. Разрешить поверх других приложений", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openOverlaySettings();
            }
        }));
        col.addView(button("2. Доступ к уведомлениям (нужен для музыки)", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "Откройте: Настройки → Уведомления → Доступ к уведомлениям", Toast.LENGTH_LONG).show();
                }
            }
        }));
        btnStart = button("3. Запустить остров", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleService();
            }
        });
        col.addView(btnStart);

        CheckBox cbClock = new CheckBox(this);
        cbClock.setText("Показывать часы, когда музыка не играет");
        cbClock.setChecked(sp.getBoolean("clock", true));
        cbClock.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton v, boolean on) {
                sp.edit().putBoolean("clock", on).apply();
                if (IslandService.instance != null) IslandService.instance.update();
            }
        });
        LinearLayout.LayoutParams lpc = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lpc.topMargin = dp(14);
        col.addView(cbClock, lpc);

        CheckBox cbBoot = new CheckBox(this);
        cbBoot.setText("Запускать при включении телефона");
        cbBoot.setChecked(sp.getBoolean("boot", true));
        cbBoot.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton v, boolean on) {
                sp.edit().putBoolean("boot", on).apply();
            }
        });
        col.addView(cbBoot);

        TextView xiaomi = label("Для Redmi / Xiaomi (чтобы остров не «засыпал»):\n"
                + "• включите «Автозапуск» для Island\n"
                + "• Экономия заряда → «Нет ограничений»\n"
                + "• в списке недавних приложений закрепите Island (замочек)", 13, false, 0xFF555555);
        xiaomi.setPadding(0, dp(16), 0, 0);
        col.addView(xiaomi);

        col.addView(button("Открыть настройки приложения (автозапуск, батарея)", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openXiaomiSettings();
            }
        }));

        setContentView(sv);

        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, 1);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshStatus();
    }

    private boolean overlayOk() {
        return Settings.canDrawOverlays(this);
    }

    private boolean listenerOk() {
        String s = Settings.Secure.getString(getContentResolver(), "enabled_notification_listeners");
        return s != null && s.contains(getPackageName());
    }

    private void refreshStatus() {
        boolean running = IslandService.instance != null;
        status.setText((overlayOk() ? "✔" : "✖") + "  Поверх других приложений\n"
                + (listenerOk() ? "✔" : "✖") + "  Доступ к уведомлениям\n"
                + (running ? "✔" : "✖") + "  Остров запущен");
        btnStart.setText(running ? "Остановить остров" : "3. Запустить остров");
    }

    private void openOverlaySettings() {
        try {
            startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName())));
        } catch (Exception e) {
            openAppDetails();
        }
    }

    private void openAppDetails() {
        try {
            startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:" + getPackageName())));
        } catch (Exception ignored) {
        }
    }

    private void openXiaomiSettings() {
        try {
            Intent i = new Intent();
            i.setComponent(new ComponentName("com.miui.securitycenter",
                    "com.miui.permcenter.autostart.AutoStartManagementActivity"));
            startActivity(i);
        } catch (Exception e) {
            openAppDetails();
        }
    }

    private void toggleService() {
        if (IslandService.instance != null) {
            stopService(new Intent(this, IslandService.class));
            sp.edit().putBoolean("enabled", false).apply();
        } else {
            if (!overlayOk()) {
                Toast.makeText(this, "Сначала разрешите показ поверх других приложений", Toast.LENGTH_LONG).show();
                openOverlaySettings();
                return;
            }
            startForegroundService(new Intent(this, IslandService.class));
            sp.edit().putBoolean("enabled", true).apply();
            if (!listenerOk()) {
                Toast.makeText(this, "Для музыки включите шаг 2 (доступ к уведомлениям)", Toast.LENGTH_LONG).show();
            }
        }
        h.postDelayed(new Runnable() {
            @Override
            public void run() {
                refreshStatus();
            }
        }, 600);
    }
}
