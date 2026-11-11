package com.islandlite;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.provider.Settings;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context ctx, Intent intent) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) return;
        SharedPreferences sp = ctx.getSharedPreferences("island", Context.MODE_PRIVATE);
        if (sp.getBoolean("boot", true) && sp.getBoolean("enabled", false) && Settings.canDrawOverlays(ctx)) {
            try {
                ctx.startForegroundService(new Intent(ctx, IslandService.class));
            } catch (Exception ignored) {
            }
        }
    }
}
