# Glide - Schedule Manager

Glide is a standalone, offline-first desktop scheduling and reminder application built with Java Swing and SQLite for CSI 2999 (Sophomore Project) at Oakland University.

**Team: Work in Progress**
Chad Lombardo, Jacob Wroblewski, Mark Dedvukaj, Roger Perez

---

## How to Download and Run (Windows 10 / 11)

No Java installation, zip extraction, or administrator permissions are required.

1. Go to the [Latest Release](https://github.com/cmlombar-create/glide-desktop/releases/latest) page on this repository.
2. Under **Assets**, click **Glide.exe** to download the standalone executable.
3. Double-click **Glide.exe** to launch Glide.
4. **Windows Defender SmartScreen note:** Because this is an unsigned student project executable, Windows may show a "Windows protected your PC" pop-up on first launch. Click **More info**, then click **Run anyway**.
5. On first launch, a brief *"Starting Glide - Schedule Manager..."* window appears for a few seconds while the embedded runtime unpacks locally. Every launch after that opens immediately.

---

## Core Features

* **Event Management (CRUD):** Create, view, edit, and delete scheduled events in three clicks or fewer.
* **Category Filtering:** Organize and filter events chronologically across 8 built-in categories (Exam, Lecture, Homework, Practice, Game, Study, Work, Personal).
* **Automated Visual Alerts:** Background 30-second polling checks upcoming deadlines and displays an always-on-top reminder pop-up before the scheduled start time.
* **100% Offline Local Storage:** All events are stored locally in an auto-initialized SQLite database (glide.db) with parameterized queries and zero network dependencies.
* **Input Validation:** Rejects blank titles, overly long text fields, and past-dated event timestamps while keeping your form entries intact so you can fix them.

---

## Tech Stack

* **Language & UI:** Java SE, Java Swing
* **Database:** Embedded SQLite 3 (Xerial sqlite-jdbc)
* **Packaging:** Self-contained Windows executable bundling the private Java runtime and SQLite driver