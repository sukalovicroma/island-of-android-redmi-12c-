package com.islandlite;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Outline;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.view.WindowManager;
import android.provider.Settings;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Date;
import java.util.List;

public class IslandService extends Service {
    static IslandService instance;
    private static final String CH = "island_channel";

    private WindowManager wm;
    private LinearLayout root;
    private GradientDrawable bg;
    private WindowManager.LayoutParams lp;
    private boolean added;

    private LinearLayout compact, full;
    private ImageView ivSmall, ivBig;
    private TextView tvSmall, tvClock, tvTitle, tvArtist;
    private BarsView bars;
    private ProgressView prog;
    private IconView btnPrev, btnPlay, btnNext;

    private final Handler h = new Handler(Looper.getMainLooper());
    private MediaSessionManager msm;
    private MediaController ctrl;

    private boolean expanded, hasTrack, playing, screenOn = true;
    private String title = "", artist = "";
    private Bitmap cover;
    private long duration;

    // ---------- жизненный цикл ----------
    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        startForeground(1, buildNotification());

        if (!Settings.canDrawOverlays(this)) {
            stopSelf();
            return;
        }

        wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        msm = (MediaSessionManager) getSystemService(Context.MEDIA_SESSION_SERVICE);
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        screenOn = pm == null || pm.isInteractive();

        buildViews();
        try {
            wm.addView(root, lp);
            added = true;
        } catch (Exception e) {
            stopSelf();
            return;
        }

        IntentFilter f = new IntentFilter();
        f.addAction(Intent.ACTION_SCREEN_ON);
        f.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(screenReceiver, f);

        refreshSessions();
        update();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        instance = null;
        h.removeCallbacksAndMessages(null);
        try {
            unregisterReceiver(screenReceiver);
        } catch (Exception ignored) {
        }
        try {
            if (msm != null) msm.removeOnActiveSessionsChangedListener(sessionsListener);
            if (ctrl != null) ctrl.unregisterCallback(cb);
        } catch (Exception ignored) {
        }
        if (added && wm != null) {
            try {
                wm.removeView(root);
            } catch (Exception ignored) {
            }
            added = false;
        }
        super.onDestroy();
    }

    private Notification buildNotification() {
        NotificationManager nm = getSystemService(NotificationManager.class);
        nm.createNotificationChannel(new NotificationChannel(CH, "Island", NotificationManager.IMPORTANCE_MIN));
        PendingIntent pi = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), PendingIntent.FLAG_IMMUTABLE);
        return new Notification.Builder(this, CH)
                .setContentTitle("Island работает")
                .setContentText("Нажмите, чтобы открыть настройки")
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setContentIntent(pi)
                .build();
    }

    // ---------- интерфейс ----------
    private int dp(float v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    private TextView text(float sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setSingleLine(true);
        t.setEllipsize(TextUtils.TruncateAt.END);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
    }

    private void roundImage(ImageView iv, int radiusDp) {
        final int r = dp(radiusDp);
        iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
        iv.setBackgroundColor(0xFF22222A);
        iv.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View v, Outline o) {
                o.setRoundRect(0, 0, v.getWidth(), v.getHeight(), r);
            }
        });
        iv.setClipToOutline(true);
    }

    private void buildViews() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        bg = new GradientDrawable();
        bg.setColor(0xFF0A0A0C);
        bg.setCornerRadius(dp(16));
        root.setBackground(bg);
        root.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggle();
            }
        });

        // --- свёрнутый вид ---
        compact = new LinearLayout(this);
        compact.setOrientation(LinearLayout.HORIZONTAL);
        compact.setGravity(Gravity.CENTER_VERTICAL);
        compact.setPadding(dp(8), 0, dp(10), 0);
        compact.setMinimumHeight(dp(32));

        ivSmall = new ImageView(this);
        roundImage(ivSmall, 6);
        compact.addView(ivSmall, new LinearLayout.LayoutParams(dp(22), dp(22)));

        tvSmall = text(11, 0xFFE6E6EC, false);
        tvSmall.setMaxWidth(dp(110));
        LinearLayout.LayoutParams lpT = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lpT.leftMargin = dp(6);
        compact.addView(tvSmall, lpT);

        bars = new BarsView(this);
        LinearLayout.LayoutParams lpB = new LinearLayout.LayoutParams(dp(14), dp(14));
        lpB.leftMargin = dp(8);
        compact.addView(bars, lpB);

        tvClock = text(13, 0xFFFFFFFF, true);
        tvClock.setGravity(Gravity.CENTER);
        tvClock.setMinWidth(dp(64));
        compact.addView(tvClock, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        root.addView(compact, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        // --- раскрытый вид ---
        full = new LinearLayout(this);
        full.setOrientation(LinearLayout.VERTICAL);
        full.setPadding(dp(14), dp(14), dp(14), dp(10));

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);

        ivBig = new ImageView(this);
        roundImage(ivBig, 14);
        top.addView(ivBig, new LinearLayout.LayoutParams(dp(64), dp(64)));

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        tvTitle = text(15, 0xFFFFFFFF, true);
        tvArtist = text(12, 0xFFAAAAB4, false);
        col.addView(tvTitle);
        col.addView(tvArtist);
        LinearLayout.LayoutParams lpCol = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        lpCol.leftMargin = dp(12);
        top.addView(col, lpCol);
        full.addView(top, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        prog = new ProgressView(this);
        LinearLayout.LayoutParams lpP = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(4));
        lpP.topMargin = dp(12);
        full.addView(prog, lpP);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        btnPrev = new IconView(this, IconView.PREV);
        btnPlay = new IconView(this, IconView.PLAY);
        btnNext = new IconView(this, IconView.NEXT);
        btnPrev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ctrl != null) ctrl.getTransportControls().skipToPrevious();
                scheduleCollapse();
            }
        });
        btnPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ctrl != null) {
                    if (playing) ctrl.getTransportControls().pause();
                    else ctrl.getTransportControls().play();
                }
                scheduleCollapse();
            }
        });
        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ctrl != null) ctrl.getTransportControls().skipToNext();
                scheduleCollapse();
            }
        });
        IconView[] btns = {btnPrev, btnPlay, btnNext};
        for (IconView b : btns) {
            LinearLayout.LayoutParams lpb = new LinearLayout.LayoutParams(dp(64), dp(44));
            lpb.leftMargin = dp(8);
            lpb.rightMargin = dp(8);
            row.addView(b, lpb);
        }
        LinearLayout.LayoutParams lpRow = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lpRow.topMargin = dp(4);
        full.addView(row, lpRow);

        full.setVisibility(View.GONE);
        root.addView(full, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        // --- окно поверх всего ---
        lp = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        lp.y = dp(4);
        if (Build.VERSION.SDK_INT >= 28) {
            lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }
    }

    // ---------- раскрытие / сворачивание ----------
    private final Runnable collapseRun = new Runnable() {
        @Override
        public void run() {
            expanded = false;
            applyMode();
        }
    };

    private void scheduleCollapse() {
        h.removeCallbacks(collapseRun);
        h.postDelayed(collapseRun, 7000);
    }

    private void toggle() {
        if (!hasTrack) return;
        expanded = !expanded;
        h.removeCallbacks(collapseRun);
        if (expanded) scheduleCollapse();
        applyMode();
    }

    // ---------- музыка ----------
    private final MediaSessionManager.OnActiveSessionsChangedListener sessionsListener =
            new MediaSessionManager.OnActiveSessionsChangedListener() {
                @Override
                public void onActiveSessionsChanged(List<MediaController> list) {
                    pickController(list);
                }
            };

    private final MediaController.Callback cb = new MediaController.Callback() {
        @Override
        public void onMetadataChanged(MediaMetadata m) {
            update();
        }

        @Override
        public void onPlaybackStateChanged(PlaybackState s) {
            update();
        }

        @Override
        public void onSessionDestroyed() {
            refreshSessions();
        }
    };

    void refreshSessions() {
        if (msm == null) return;
        try {
            ComponentName cn = new ComponentName(this, MediaListenerService.class);
            msm.removeOnActiveSessionsChangedListener(sessionsListener);
            msm.addOnActiveSessionsChangedListener(sessionsListener, cn);
            pickController(msm.getActiveSessions(cn));
        } catch (SecurityException e) {
            // нет доступа к уведомлениям — просто показываем часы
            pickController(null);
        } catch (Exception ignored) {
        }
    }

    private void pickController(List<MediaController> list) {
        MediaController best = null;
        if (list != null) {
            for (MediaController c : list) {
                PlaybackState ps = c.getPlaybackState();
                if (ps != null && ps.getState() == PlaybackState.STATE_PLAYING) {
                    best = c;
                    break;
                }
                if (best == null) best = c;
            }
        }
        if (ctrl != null && best != null && ctrl.getSessionToken().equals(best.getSessionToken())) {
            update();
            return;
        }
        if (ctrl != null) {
            try {
                ctrl.unregisterCallback(cb);
            } catch (Exception ignored) {
            }
        }
        ctrl = best;
        if (ctrl != null) ctrl.registerCallback(cb);
        update();
    }

    void update() {
        String t = "", a = "";
        Bitmap c = null;
        long dur = 0;
        boolean pl = false;
        if (ctrl != null) {
            MediaMetadata m = ctrl.getMetadata();
            if (m != null) {
                t = nz(m.getString(MediaMetadata.METADATA_KEY_TITLE));
                a = nz(m.getString(MediaMetadata.METADATA_KEY_ARTIST));
                if (a.length() == 0) a = nz(m.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST));
                c = m.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART);
                if (c == null) c = m.getBitmap(MediaMetadata.METADATA_KEY_ART);
                if (c == null) c = m.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON);
                dur = m.getLong(MediaMetadata.METADATA_KEY_DURATION);
            }
            PlaybackState ps = ctrl.getPlaybackState();
            pl = ps != null && ps.getState() == PlaybackState.STATE_PLAYING;
        }
        title = t;
        artist = a;
        cover = c;
        duration = dur;
        playing = pl;
        hasTrack = t.length() > 0 || a.length() > 0;
        if (!hasTrack) expanded = false;
        applyMode();
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }

    // ---------- отрисовка состояния ----------
    private void applyMode() {
        if (!added) return;
        boolean clockPref = getSharedPreferences("island", MODE_PRIVATE).getBoolean("clock", true);
        boolean playerCompact = hasTrack && playing;

        // свёрнутый вид
        ivSmall.setVisibility(playerCompact && cover != null ? View.VISIBLE : View.GONE);
        tvSmall.setVisibility(playerCompact ? View.VISIBLE : View.GONE);
        bars.setVisibility(playerCompact ? View.VISIBLE : View.GONE);
        tvClock.setVisibility(playerCompact ? View.GONE : View.VISIBLE);
        if (playerCompact) {
            tvSmall.setText(title.length() > 0 ? title : artist);
            ivSmall.setImageBitmap(cover);
        } else {
            tvClock.setText(DateFormat.getTimeFormat(this).format(new Date()));
        }

        // раскрытый вид
        if (expanded) {
            tvTitle.setText(title.length() > 0 ? title : "Без названия");
            tvArtist.setText(artist);
            ivBig.setImageBitmap(cover);
            btnPlay.setType(playing ? IconView.PAUSE : IconView.PLAY);
            updateProgress();
        }
        compact.setVisibility(expanded ? View.GONE : View.VISIBLE);
        full.setVisibility(expanded ? View.VISIBLE : View.GONE);
        bg.setCornerRadius(dp(expanded ? 28 : 16));

        DisplayMetrics dm = getResources().getDisplayMetrics();
        lp.width = expanded ? Math.min(dp(340), dm.widthPixels - dp(24)) : WindowManager.LayoutParams.WRAP_CONTENT;

        boolean hide = !expanded && !playerCompact && !clockPref;
        root.setVisibility(hide ? View.GONE : View.VISIBLE);
        try {
            wm.updateViewLayout(root, lp);
        } catch (Exception ignored) {
        }
        scheduleTicker();
    }

    private void updateProgress() {
        long pos = 0;
        PlaybackState ps = ctrl != null ? ctrl.getPlaybackState() : null;
        if (ps != null) {
            pos = ps.getPosition();
            if (ps.getState() == PlaybackState.STATE_PLAYING && ps.getLastPositionUpdateTime() > 0) {
                long el = SystemClock.elapsedRealtime() - ps.getLastPositionUpdateTime();
                pos += (long) (el * ps.getPlaybackSpeed());
            }
        }
        prog.setFraction(duration > 0 ? (float) pos / (float) duration : 0f);
    }

    // ---------- таймер: только когда экран включён ----------
    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            if (expanded) {
                updateProgress();
            } else if (tvClock.getVisibility() == View.VISIBLE) {
                tvClock.setText(DateFormat.getTimeFormat(IslandService.this).format(new Date()));
            }
            scheduleTicker();
        }
    };

    private void scheduleTicker() {
        h.removeCallbacks(ticker);
        if (!screenOn) return;
        long delay;
        if (expanded && hasTrack) {
            delay = 1000;
        } else {
            long now = System.currentTimeMillis();
            delay = 60000 - (now % 60000) + 50;
        }
        h.postDelayed(ticker, delay);
    }

    private final BroadcastReceiver screenReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
                screenOn = false;
                h.removeCallbacks(ticker);
            } else if (Intent.ACTION_SCREEN_ON.equals(intent.getAction())) {
                screenOn = true;
                update();
            }
        }
    };
}
