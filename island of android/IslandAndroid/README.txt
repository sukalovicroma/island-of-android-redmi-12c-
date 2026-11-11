Island для Android (Redmi 12C) — как получить приложение (APK)

Сборка идёт на сайте GitHub бесплатно, ставить ничего не нужно.

1. На ноутбуке откройте github.com и создайте бесплатный аккаунт.
2. Нажмите "+" вверху справа -> New repository. Имя: island. Выберите Public. Create repository.
3. На странице репозитория нажмите ссылку "uploading an existing file".
4. Откройте распакованную папку IslandAndroid и перетащите в окно браузера ВСЁ её содержимое
   (папку app, папку .github, файлы build.gradle, settings.gradle, gradle.properties).
   Нажмите зелёную кнопку Commit changes.
   Если папка .github не загрузилась: Add file -> Create new file, в имени напишите
   .github/workflows/build.yml и вставьте туда содержимое файла build.yml из папки .github/workflows.
5. Откройте вкладку Actions. Сборка "Build APK" стартует сама (если просит - нажмите
   "I understand my workflows, go ahead and enable them"). Ждать 3-6 минут до зелёной галочки.
6. Нажмите на завершённую сборку, внизу в разделе Artifacts скачайте IslandLite-apk.
   Внутри zip лежит app-debug.apk.
7. Перекиньте app-debug.apk на телефон (например в Telegram "Избранное"), откройте, разрешите
   установку из этого источника. Если Play Protect предупредит - "Всё равно установить".
8. Откройте Island, пройдите шаги 1-2 (два разрешения), нажмите "Запустить остров".
