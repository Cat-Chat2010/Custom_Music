# 🎵 Custom Music Mod for Minecraft 1.21.1 (NeoForge)

<p align="center">
  <img src="https://img.shields.io/badge/Minecraft-1.21.1-brightgreen.svg" alt="Minecraft 1.21.1">
  <img src="https://img.shields.io/badge/NeoForge-21.1.65+-orange.svg" alt="NeoForge">
  <img src="https://img.shields.io/badge/Java-21-blue.svg" alt="Java 21">
  <img src="https://img.shields.io/badge/License-MIT-yellow.svg" alt="License">
  <img src="https://img.shields.io/badge/Side-Client-purple.svg" alt="Client-Side Only">
</p>

---

## 🇬🇧 English

A comprehensive client-side music manager and player for **Minecraft 1.21.1 (NeoForge)**. It allows you to play your own custom tracks, manage playlists, and listen to all official Minecraft music with an in-game HUD mini-player and seamless background playback.

### 🤖 AI Development Disclosure
This project was developed by **Cat-Chat2010** with the assistance of **Artificial Intelligence (Google Antigravity / AI Pair Programming)** for code architecture, mixins, audio streaming engine, and documentation.

* 📂 **Albums & Playlists System:**
  * Create custom albums and playlists with custom names (defaults to sequential numbers `1`, `2`... if left blank).
  * Permanent built-in album **"All Tracks"** contains all songs and cannot be deleted.
  * Search tracks by title in real-time (`🔍 Search...`).
  * Flexible sorting modes: **By Name**, **By Duration**, **By Type** (`[Custom]`, `[Vanilla]`, `[Disc]`), or **Custom** (manual Drag-and-Drop order persisted in config).
  * Shuffle songs inside the current album with `🔀 Shuffle`.
  * Add and remove tracks to/from custom albums with simple buttons.
* 🎛️ **Dual-Tab Interface (`Ctrl + P`):**
  * **Albums Tab (`💿 Albums`):** Integrated interactive player with scrubber, rewind (`-5s` / `+5s`), previous/next track buttons, search, and album playlist.
  * **Settings Tab (`⚙ Settings`):** Silence delay sliders, volume controls, file import, file deletion, and individual track volume settings.
* 🔊 **Multi-Level Volume Control:**
  * **Volume by Category:** Independent volume sliders (0% - 100%) for Custom Music, Vanilla Soundtrack, and Music Discs.
  * **Individual Track Volume:** Custom volume slider for any specific track. Moving slider to 100% automatically removes it from the custom volume list.
  * **Real-Time Audio Adjustment:** Moving sliders immediately adjusts OpenAL channel volume without lag or audio stutter.
* 📁 **In-Game File Management:**
  * **Native File Picker:** Click "Add Music" to pick `.mp3` or `.ogg` files via the standard Windows Explorer dialog.
  * **Delete Music:** Dedicated searchable screen with `[✕]` buttons to remove unwanted custom audio files directly from disk.
* 📌 **HUD Mini-Player (`Ctrl + O`):**
  * Compact on-screen overlay (96x34) in the top-right corner.
  * Real-time timeline: elapsed time, remaining time (`-X:XX`), total track duration, play/pause icon, and clean progress bar.
  * Smooth slide-in / slide-out animations. Permanent display mode (`Ctrl + O`) or automatic 7-second popup on track change.
* ⚡ **Hardware-Accelerated Zero-Lag Rewind & Stream Ending:**
  * 30-second circular PCM history buffer enables instantaneous `-5s` rewind with 0 ms latency and 0% CPU overhead.
  * Robust EOF stream handling prevents infinite empty OpenAL buffer playback.
* ⚡ **Native MP3 & OGG Playback (No FFmpeg Needed):**
  * Direct playback of `.mp3` and `.ogg` files via embedded pure-Java audio decoding into Minecraft's OpenAL engine.
  * No external software, commands, or codecs required — works 100% out of the box!
* 🌐 **Full Multi-Language Support:**
  * Native in-game localization for English (`en_us.json`), Ukrainian (`uk_ua.json`), and Russian (`ru_ru.json`).
  * Automatic English fallback for all other languages selected in Minecraft.
* 🛡️ **User File Safety:**
  * Your original music files are never altered, moved, or deleted — only safe working copies are created.

### ⌨️ Controls
* **`Ctrl + P`** — Open Music Settings & Track List
* **`Ctrl + O`** — Toggle HUD Mini-Player

### 📂 Folder Structure
```text
.minecraft/
└── custom_music/
    ├── your_song_1.mp3   <-- Drop your .mp3 tracks here
    ├── your_song_2.ogg   <-- Drop your .ogg tracks here
    └── custommusic.json  <-- Configuration and playlist order
```

### 🎵 Adding Music
* **In-Game:** Press **`Ctrl + P`** and click **"📁 Add Music"** to pick files using the native file dialog.
* **Manually:** Simply copy any `.mp3` or `.ogg` audio files into `.minecraft/custom_music/`.

### 🔨 Building from Source
* Requirements: **JDK 21**
* Build jar:
  ```cmd
  gradlew.bat clean build
  ```
* Output jar is located in `build/libs/`.

### 📜 License
Distributed under the **MIT License**.

---

## 🇺🇦 Українська

Повноцінний клієнтський музичний менеджер і плеєр для **Minecraft 1.21.1 (NeoForge)**. Дозволяє додавати власні аудіотреки, керувати плейлистами та слухати всю офіційну музику Minecraft зі стильним HUD міні-плеєром і безперервним фоновим відтворенням.

### 🤖 Створено за допомогою ШІ (AI Disclosure)
Цей проєкт створений автором **Cat-Chat2010** за активної участі **штучного інтелекту (Google Antigravity / AI Pair Programming)** для розробки архітектури коду, міксинів, звукового рушія та документації.

* 📂 **Система альбомів та плейлистів:**
  * Створення власних альбомів і плейлистів з можливістю введення назви (якщо залишити порожнім, автоматично створюються послідовні номери `1`, `2`...).
  * Постійний альбом **«Усі пісні»**, який містить усі треки і не може бути видалений.
  * Пошук треків за назвою в реальному часі (`🔍 Пошук...`).
  * Зручні режими упорядкування: **За назвою**, **За довжиною**, **За типом** (`[Custom]`, `[Vanilla]`, `[Disc]`) або **Своє** (ручний порядок через Drag-and-Drop, який зберігається у конфігурації).
  * Перемішування пісень в обраному альбомі кнопкою `🔀 Перемішати`.
  * Додавання пісень до кастомного альбому та прибирання з нього за допомогою кнопок.
* 🎛️ **Двокомпонентний інтерфейс (`Ctrl + P`):**
  * **Вкладка «Альбоми» (`💿 Альбоми`):** Вбудований інтерактивний плеєр із таймлайном, перемоткою (`-5s` / `+5s`), кнопками зміни треків, пошуком та списком пісень альбому.
  * **Вкладка «Налаштування» (`⚙ Налаштування`):** Слайдери затримок тиші, регулювання гучності, імпорт та видалення файлів, персональна гучність треків.
* 🔊 **Багаторівневе регулювання гучності:**
  * **Гучність за категоріями:** Окремі повзунки (0% - 100%) для кастомних пісень, ванільного саундтреку та музичних платівок.
  * **Індивідуальна гучність окремих пісень:** Можливість налаштувати тихіше будь-який окремий трек. При піднятті гучності до 100% трек автоматично прибирається зі списку налаштувань.
  * **Миттєве оновлення:** Зміна повзунка одразу ж оновлює гучність каналу OpenAL у реальному часі.
* 📁 **Керування файлами з гри:**
  * **Нативний провідник:** Кнопка «Додати музику» відкриває системний діалог Windows Explorer для вибору файлів `.mp3` та `.ogg`.
  * **Видалення файлів:** Спеціальний екран зі списком та пошуком для безпечного видалення кастомних файлів з диску через кнопку `[✕]`.
* 📌 **HUD Міні-плеєр (`Ctrl + O`):**
  * Компактна плашка (96x34) у правому верхньому куті екрана.
  * Точний таймлайн у реальному часі: пройдений час, скільки залишилося (`-хв:сек`), загальна тривалість, значок плей/паузи та смужка прогресу.
  * Плавна анімація (slide-in / slide-out). Режим постійного закріплення (`Ctrl + O`) або автоматична поява на 7 секунд при старті нової пісні.
* ⚡ **Миттєва апаратна перемотка без лагів та коректне завершення треків:**
  * 30-секундний кільцевий PCM-буфер історії пам'яті для миттєвої перемотки назад на 5 секунд без повторного декодування з нульовим навантаженням на CPU (0 мс).
  * Коректне повернення EOF для запобігання зацикленню пустих OpenAL буферів після закінчення пісні.
* 🔊 **Безперервне фонове відтворення:**
  * Музика не зупиняється і не ставиться на паузу при відкритті меню паузи, налаштувань гри чи меню мода.
* 🎶 **Підтримка всіх 79 ванільних треків Minecraft 1.21.1:**
  * Фіксовані шляхи до звуків для всього саундтреку (Overworld, Nether, End, Creative, Menu).
  * Музичні платівки (`Pigstep`, `Cat`, `Otherside`, `Creator`, `Precipice`, `Relic` тощо) грають свою точну мелодію без випадкового вибору.
  * Кольорові бейджі: `[Custom]` (блакитний), `[Vanilla]` (сірий) та `[Disc]` (золотий для платівок).
  * Точне авторство треків: Aaron Cherof (*Precipice*, *Featherfall*), Lena Raine (*Endless*, *Creator*) та інші.
* ⚡ **Нативне відтворення MP3 та OGG (без FFmpeg):**
  * Пряме програвання файлів `.mp3` та `.ogg` завдяки вбудованому Java-декодеру в OpenAL рушій Minecraft.
  * Жодних сторонніх програм, кодеків чи консольних утиліт — працює на 100% з коробки!
* 🌐 **Повноцінна багатомовна локалізація:**
  * Повна підтримка англійської (`en_us.json`), української (`uk_ua.json`) та російської (`ru_ru.json`) мов інтерфейсу.
  * Автоматичне використання англійської мови як резервної для будь-яких інших мов гри.
* 🛡️ **Безпека файлів користувача:**
  * Ваші оригінальні аудіофайли ніколи не видаляються і не змінюються — створюються лише робочі копії.

### ⌨️ Керування
* **`Ctrl + P`** — Відкрити меню налаштувань музики
* **`Ctrl + O`** — Увімкнути / вимкнути HUD міні-плеєр

### 📂 Структура папок
```text
.minecraft/
└── custom_music/
    ├── vasha_pisnya_1.mp3 <-- Сюди додаються ваші .mp3 треки
    ├── vasha_pisnya_2.ogg <-- Сюди додаються ваші .ogg треки
    └── custommusic.json   <-- Файл конфігурації та збереженого порядку
```

### 🎵 Як додавати музику
* **Прямо у грі:** Натисніть **`Ctrl + P`** та оберіть **«📁 Додати музику»** для вибору файлів через системний провідник.
* **Вручну:** Просто скопіюйте будь-які аудіофайли `.mp3` або `.ogg` у папку `.minecraft/custom_music/`.

### 🔨 Збірка з вихідного коду
* Вимоги: **JDK 21**
* Команда збірки:
  ```cmd
  gradlew.bat clean build
  ```
* Готовий файл `.jar` буде в папці `build/libs/`.

### 📜 Ліцензія
Поширюється за ліцензією **MIT License**.
