# GLITCH Pocket Lab — Android

Projekt zawiera oryginalny aparat Pocket Lab, jego podgląd efektów LIVE i edytor GLITCH/MACHINE v37. Jest projektem do kompilacji, nie gotowym APK; nie został przetestowany na fizycznym telefonie.

## GitHub Actions

Umieść zawartość tego folderu w katalogu głównym repozytorium, łącznie z ukrytym `.github/workflows/android.yml`. Po wypchnięciu gałęzi `main`, w zakładce Actions uruchomi się `Build Android APK`. Po sukcesie pobierz artefakt `GLITCH-Pocket-debug-APK`; w ZIP-ie znajduje się `app-debug.apk`.

APK jest kompilacją debugową podpisaną kluczem wygenerowanym przez narzędzia Android. Kolejne kompilacje na różnych maszynach mogą być podpisane innym kluczem i wymagać usunięcia starej wersji przed instalacją. Usunięcie aplikacji usuwa jej lokalne ustawienia.

W aplikacji wciśnij APARAT LIVE → Włącz aparat i zezwól Androidowi na dostęp do kamery. Efekt LIVE włącza się osobnym przyciskiem. Po zrobieniu i przeliczeniu zdjęcia użyj „Przekaż do edytora”. Eksport edytora zapisuje do Pobrane/GLITCH_MACHINE. Import używa systemowego wyboru plików.

Projekt wymaga Androida 10 lub nowszego (API 29) oraz aktualnego Android System WebView. Dostęp do kamery jest ograniczony do lokalnego zaufanego originu. Nie zbiera ani nie wysyła zdjęć do Internetu. Budowanie wymaga JDK 17, Gradle 8.9, Android SDK 35 i pobrania zależności.
