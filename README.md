# Glide - Schedule Manager

Glide is a standalone, offline-first desktop scheduling and reminder application built with Java Swing and SQLite for CSI 2999 (Sophomore Project) at Oakland University.

**Team: Work in Progress**
Chad Lombardo, Jacob Wroblewski, Mark Dedvukaj, Roger Perez

---

## What Glide Does

Glide gives student-athletes a single offline dashboard to organize classes, exams, practices, games, study sessions, and work shifts without needing an internet connection or cloud account. It validates schedule entries, filters commitments chronologically by category, triggers audible and visual pop-up alerts ahead of deadlines, and saves all changes automatically to a local SQLite database.

---

## How to Download and Run (Windows 10 / 11)

No Java installation or administrator permissions are required.

1. Go to the [Latest Release](https://github.com/cmlombar-create/glide-desktop/releases/latest) page on this repository.
2. Under **Assets**, click **`Glide-Windows-Portable.zip`** to download the portable bundle.
3. Right-click **`Glide-Windows-Portable.zip`**, choose **Extract All...**, and click **Extract**.
4. Open the extracted **`Glide`** folder and double-click **`Glide`** (`Glide.exe`) to launch the application.
5. **Windows SmartScreen note:** If Windows displays a *"Windows protected your PC"* prompt on first launch, click **More info** and then click **Run anyway**.

---

## How to Use Glide

* **Add an Event:** Click **+ Add Event** on the bottom toolbar, enter an Event Title, choose one of the 8 Categories (`Exam`, `Lecture`, `Homework`, `Practice`, `Game`, `Study`, `Work`, `Personal`), enter a future Date & Time (`MM/dd/yyyy hh:mm AM/PM`), choose a Reminder Offset, add optional Notes, and click **Save Event**.
* **Edit an Event:** Double-click any row in the schedule (or select a row and click **Edit Selected**), modify any field, and click **Save Event**. Updating the date/time or reminder offset automatically re-arms the reminder.
* **Clone an Event:** Select an existing event row and click **Clone Selected**. Glide opens the dialog pre-filled with that event's details so you can pick a new date/time for recurring classes, practices, or shifts and click **Save Event**.
* **Filter by Category:** Use the **Category Filter** dropdown in the top bar to isolate a single category or select `All Categories` to view your entire chronological schedule.
* **Delete an Event:** Select an event row, click **Delete**, and click **Yes** to permanently remove it.
* **Audio & Pop-up Reminders:** Leave Glide open while studying or working. When an event reaches its reminder offset, Glide plays a two-tone notification chime and displays an always-on-top **Upcoming Event Reminder** dialog. Click **Dismiss** to close the alert; the event status automatically updates from `Scheduled` to `Completed`.
* **Persistent Offline Storage:** Every addition, edit, clone, and deletion is saved immediately to `%LOCALAPPDATA%\Glide\glide.db`, keeping your schedule intact whenever you close and reopen the app.

---



---

## Tech Stack

* **Language & UI:** Java SE 17+, Java Swing
* **Database:** Embedded SQLite 3 (Xerial `sqlite-jdbc`)
* **Packaging:** Native OpenJDK `jpackage` Windows runtime bundle (`Glide.exe` + private JRE)
