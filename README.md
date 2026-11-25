# Glide - Schedule Manager

Glide is a standalone, offline-first desktop scheduling and reminder application built with Java Swing and SQLite for CSI 2999 (Sophomore Project) at Oakland University.

**Team: Work in Progress**
Chad Lombardo, Jacob Wroblewski, Mark Dedvukaj, Roger Perez

---

## How to Download and Run (Windows 10 / 11)

No Java installation or administrator permissions are required.

1. Go to the [Latest Release](https://github.com/cmlombar-create/glide-desktop/releases/latest) page on this repository.
2. Under **Assets**, click **`Glide-Windows-Portable.zip`** to download the portable bundle.
3. Right-click **`Glide-Windows-Portable.zip`**, choose **Extract All...**, and click **Extract**.
4. Open the extracted **`Glide`** folder and double-click **`Glide.exe`** to launch Glide.
5. **Windows SmartScreen note:** If Windows shows a *"Windows protected your PC"* prompt on first launch, click **More info** and then click **Run anyway**.

---

## Core Features

* **Event Management & Cloning:** Create, view, edit, clone (**Clone Selected**), and delete scheduled events in three clicks or fewer.
* **Category Filtering:** Organize and filter events chronologically across 8 built-in categories (`Exam`, `Lecture`, `Homework`, `Practice`, `Game`, `Study`, `Work`, `Personal`).
* **Audio & Visual Reminder Alerts:** Background polling triggers an audible two-tone notification chime alongside an always-on-top Black & Gold alert dialog before scheduled deadlines.
* **Persistent Offline Storage:** Stores your schedule locally in `%LOCALAPPDATA%\Glide\glide.db` using parameterized SQLite queries so additions, edits, and deletions persist across restarts with zero network dependencies.
* **Input Validation:** Rejects blank titles, overly long text fields, and past-dated timestamps while preserving form entries so you can correct them immediately.

---

## Tech Stack

* **Language & UI:** Java SE 17+, Java Swing
* **Database:** Embedded SQLite 3 (Xerial `sqlite-jdbc`)
* **Packaging:** Native OpenJDK `jpackage` Windows runtime bundle (`Glide.exe` + private JRE)