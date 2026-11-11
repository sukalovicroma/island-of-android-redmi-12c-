package com.islandlite;

import android.service.notification.NotificationListenerService;

/**
 * Пустой «слушатель уведомлений». Он нужен только затем, чтобы Android разрешил
 * приложению читать, какая музыка играет (обложка, название, кнопки).
 * Уведомления мы не читаем и никуда не отправляем.
 */
public class MediaListenerService extends NotificationListenerService {
    @Override
    public void onListenerConnected() {
        super.onListenerConnected();
        IslandService s = IslandService.instance;
        if (s != null) s.refreshSessions();
    }
}
