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

A client-side music manager and player for **Minecraft 1.21.1 (NeoForge)**. Play your own custom tracks, manage playlists, listen to all official Minecraft music, and control playback with an in-game HUD mini-player.

### 🤖 AI Development Disclosure
Created by **Cat-Chat2010** with **Artificial Intelligence (Google Antigravity / AI Pair Programming)** for code architecture, audio engine, mixins, and documentation.

### ✨ Features
* 📂 **Playlists & Albums:** Create and manage custom albums (defaults to numbers `1`, `2`... if unnamed), permanent **"All Tracks"** playlist, real-time search, sorting (by name, duration, type, or custom drag-and-drop order), and shuffle.
* 🎛️ **Dual-Tab GUI (`Ctrl + P`):**
  * **Albums:** Interactive player with scrubber timeline, rewind (`-5s` / `+5s`), previous/next buttons, and track rotation toggles (`[✔]` / `[✕]`).
  * **Settings:** Silence delay sliders, category volumes, individual song volume sliders, and in-game file management.
* 🔊 **Audio Controls:** Independent volume sliders for Custom Music, Vanilla Soundtrack, and Music Discs. Individual volume control for any song (auto-cleans from list when set back to 100%).
* 📁 **In-Game File Management:** Add `.mp3` and `.ogg` files via native Windows Explorer dialog, or delete unwanted tracks with confirmation. Original user files are never moved or deleted.
* 📌 **HUD Mini-Player (`Ctrl + O`):** Compact on-screen overlay (96x34) in the top-right corner with timeline, remaining time, duration, and play/pause indicator. Toggle permanently with `Ctrl + O` or view automatic 7-second popup on track change.
* ⚡ **Seamless Playback & Native Engine:** Pure Java decoding for `.mp3` and `.ogg` directly into OpenAL (no FFmpeg needed). Music continues uninterrupted through pause menus, options, and dimension loading screens (Nether/End).
* 🎶 **All 79 Vanilla Tracks & Discs:** Full support for official Minecraft 1.21.1 soundtracks and music discs with accurate titles, durations, and category badges (`[Custom]`, `[Vanilla]`, `[Disc]`).
* 🌐 **Multi-Language:** Built-in English (`en_us`), Ukrainian (`uk_ua`), and Russian (`ru_ru`) with automatic English fallback.

### ⌨️ Controls
| Hotkey | Description |
| :--- | :--- |
| **`Ctrl + P`** | Open Music Manager & Settings |
| **`Ctrl + O`** | Toggle HUD Mini-Player |

### 📂 File Location
Drop `.mp3` or `.ogg` tracks into:
```text
.minecraft/custom_music/
```

### 🔨 Building from Source
Requires **JDK 21**:
```cmd
gradlew.bat clean build
```
Built JAR is located in `build/libs/`.

### 📜 License
Distributed under the **MIT License**.

---

## 🇺🇦 Українська

Клієнтський музичний менеджер і плеєр для **Minecraft 1.21.1 (NeoForge)**. Дозволяє слухати власні треки, створювати плейлисти, відтворювати всю офіційну музику Minecraft та керувати відтворенням через зручний HUD міні-плеєр.

### 🤖 Створено за допомогою ШІ (AI Disclosure)
Створено автором **Cat-Chat2010** за участі **штучного інтелекту (Google Antigravity / AI Pair Programming)** для розробки архітектури коду, звукового рушія, міксинів та документації.

### ✨ Можливості мода
* 📂 **Альбоми та плейлисти:** Створення власних альбомів (номери `1`, `2`... якщо назву не введено), постійний альбом **«Усі пісні»**, пошук у реальному часі, гнучке упорядкування (за назвою, тривалістю, типом або свій порядок через перетягування) та перемішування.
* 🎛️ **Двокомпонентний інтерфейс (`Ctrl + P`):**
  * **Альбоми:** Інтерактивний плеєр із таймлайном, перемоткою (`-5s` / `+5s`), зміною треків та перемикачами ротації пісень (`[✔]` / `[✕]`).
  * **Налаштування:** Затримки тиші між треками, регулювання гучності, персональна гучність окремих пісень та керування файлами.
* 🔊 **Контроль гучності:** Окремі повзунки для кастомної музики, ванільного саундтреку та платівок. Налаштування персональної гучності для будь-якої пісні (автоматично прибирається зі списку при 100%).
* 📁 **Керування файлами з гри:** Додавання `.mp3` та `.ogg` через системний провідник Windows, видалення файлів з пошуком та підтвердженням. Оригінальні файли користувача залишаються недоторканими.
* 📌 **HUD Міні-плеєр (`Ctrl + O`):** Компактна плашка (96x34) у верхньому правому куті з таймлайном, часом до кінця, тривалістю та значком статусу. Закріплення комбінацією `Ctrl + O` або автоматична поява на 7 секунд при старті треку.
* ⚡ **Безперервне відтворення та нативний рушій:** Чистий Java-декодер для `.mp3` та `.ogg` через OpenAL без FFmpeg. Музика не переривається під час відкриття налаштувань, меню паузи та переходів між вимірами (Недер/Енд).
* 🎶 **Усі 79 ванільних треків та платівок:** Повна підтримка офіційного саундтреку Minecraft 1.21.1 з точними назвами, авторами, тривалістю та бейджами (`[Custom]`, `[Vanilla]`, `[Disc]`).
* 🌐 **Локалізація:** Повна підтримка української (`uk_ua`), англійської (`en_us`) та російської (`ru_ru`) мов.

### ⌨️ Керування
| Комбінація | Призначення |
| :--- | :--- |
| **`Ctrl + P`** | Відкрити меню налаштувань музики |
| **`Ctrl + O`** | Закріпити / приховати HUD міні-плеєр |

### 📂 Папка для музики
Файли `.mp3` або `.ogg` додаються до:
```text
.minecraft/custom_music/
```

### 🔨 Збірка з вихідного коду
Потрібна **JDK 21**:
```cmd
gradlew.bat clean build
```
Готовий JAR буде у папці `build/libs/`.

### 📜 Ліцензія
Поширюється за ліцензією **MIT License**.
