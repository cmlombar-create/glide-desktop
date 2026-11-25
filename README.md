# Glide - Schedule Manager

Glide is a standalone, offline-first desktop scheduling and reminder application built with Java Swing and SQLite for CSI 2999 (Sophomore Project) at Oakland University.

**Team: Work in Progress**
Chad Lombardo, Jacob Wroblewski, Mark Dedvukaj, Roger Perez

---

## Overview

Glide gives student-athletes a single offline dashboard to track classes, exams, practices, games, study sessions, and work shifts without needing an internet connection or cloud account. It validates schedule inputs, organizes commitments chronologically, filters by category, triggers audible and visual pop-up alerts before deadlines, and saves every change automatically to a local SQLite database.

---

## How to Download and Run (Windows 10 / 11)

No Java installation or administrator permissions are required.

1. Go to the [Latest Release](https://github.com/cmlombar-create/glide-desktop/releases/latest) page on this repository.
2. Under **Assets**, click **`Glide-Windows-Portable.zip`** to download the portable bundle.
3. Right-click **`Glide-Windows-Portable.zip`**, select **Extract All...**, and click **Extract**.
4. Open the extracted **`Glide`** folder and double-click **`Glide`** (`Glide.exe`) to launch the application.
5. **Windows SmartScreen note:** Because this is an unsigned student build, Windows may show a *"Windows protected your PC"* prompt on first launch. Click **More info**, then click **Run anyway**.

---

## User Guide: How to Use Glide

### 1. Understanding the Dashboard
* **Top Bar (`Category Filter`):** Filters the schedule view between `All Categories` and any of the 8 built-in categories.
* **Schedule Table:** Displays all scheduled events in chronological order (earliest to latest) across five columns: `Title`, `Date & Time`, `Category`, `Reminder Lead`, and `Status` (`Scheduled` or `Completed`).
* **Viewing Event Notes:** Hover your mouse cursor over any row in the table to view that event's saved description notes in a pop-up tooltip.
* **Bottom Toolbar:** Provides one-click access to **+ Add Event**, **Edit Selected**, **Clone Selected**, **Delete**, and **Refresh**.

### 2. Adding a New Event
1. Click the gold **`+ Add Event`** button on the bottom toolbar.
2. **Event Title:** Enter a title between 1 and 100 characters (required).
3. **Category:** Choose one of the 8 categories from the dropdown (`Exam`, `Lecture`, `Homework`, `Practice`, `Game`, `Study`, `Work`, `Personal`).
4. **Date & Time:** Defaults to the next upcoming hour. Enter any future date and time using 12-hour AM/PM format (`MM/dd/yyyy hh:mm AM/PM`, such as `11/26/2026 02:30 PM`) or 24-hour format (`yyyy-MM-dd HH:mm`, such as `2026-11-26 14:30`).
5. **Reminder Offset:** Defaults to `15 Mins Before`. Select when Glide should alert you (`At Event Time`, `5 Mins Before`, `10 Mins Before`, `15 Mins Before`, `30 Mins Before`, `1 Hour Before`, `2 Hours Before`, or `1 Day Before`).
6. **Description (Optional):** Enter up to 500 characters of notes (for example, room numbers, equipment needed, or exam topics).
7. Click **`Save Event`** (or press **Enter**). If a required field is missing or the date/time is in the past, Glide lists all validation issues in a pop-up without clearing your form so you can fix them right away.

### 3. Editing an Existing Event
1. **Double-click** any event row in the table, or click a row once to highlight it in gold and click **`Edit Selected`**.
2. Modify the title, category, date/time, reminder offset, or description.
3. Click **`Save Event`**.
* **Smart Reminder Reset:** If an event is already marked `Completed` and you change its **Date & Time** or **Reminder Offset**, Glide automatically resets its status back to `Scheduled` so the alert triggers again at the updated time.

### 4. Cloning an Event (For Recurring Classes, Practices, or Shifts)
1. Click an existing event row in the table to highlight it, then click **`Clone Selected`** on the bottom toolbar.
2. The **Clone Event** dialog opens pre-filled with that event's title (`(Copy)` added), category, reminder offset, and description. (If the original event's timestamp has already passed, Glide automatically advances the suggested date to tomorrow at the same time.)
3. Adjust the **Date & Time** (or any other field) for the next occurrence and click **`Save Event`**.

### 5. Filtering by Category & Refreshing
* **Filter Schedule:** Click the **`Category Filter`** dropdown in the top bar and select a specific category (such as `Exam` or `Practice`) to view only matching commitments. Select **`All Categories`** to return to your full schedule.
* **Manual Refresh:** Click **`Refresh`** on the bottom toolbar at any time to reload the current view from the database.

### 6. Receiving & Testing Audio + Pop-up Reminders
* Keep Glide open (either visible or minimized in the background). Glide checks active events once every second.
* When the system clock reaches an event's reminder time (`Date & Time` minus `Reminder Offset`), Glide plays a two-tone notification chime and displays an always-on-top **Upcoming Event Reminder** window showing the event title, category, scheduled time, countdown, and notes.
* Click **`Dismiss`** to close the alert. The event's `Status` changes from `Scheduled` to `Completed` so it will not alert you a second time.
* **Missed-Alert Protection:** If Glide was closed during an event and you reopen the app more than 5 minutes after the scheduled start time, Glide marks the past event `Completed` without showing an outdated pop-up.
* **Quick 10-Second Live Test (For Grading & Demos):** Click **`+ Add Event`**, enter `Test Alert`, set **Date & Time** to **2 minutes from the current clock time**, set **Reminder Offset** to **`5 Mins Before`**, and click **`Save Event`**. Because the current time is already inside the 5-minute reminder window, the audio chime and pop-up alert will trigger immediately.

### 7. Deleting an Event
1. Click an event row in the table to select it, then click **`Delete`** on the bottom toolbar.
2. Click **`Yes`** in the confirmation prompt to permanently remove the event.

### 8. Automatic Offline Saving
* Glide saves every addition, edit, clone, status update, and deletion immediately to a local SQLite database at `%LOCALAPPDATA%\Glide\glide.db`.
* You can close and reopen Glide at any time without losing your schedule or needing to click a separate save button.

---

## Tech Stack

* **Language & UI:** Java SE 17+, Java Swing
* **Database:** Embedded SQLite 3 (Xerial `sqlite-jdbc`) with parameterized `PreparedStatement` queries
* **Packaging:** Native OpenJDK `jpackage` Windows runtime bundle (`Glide.exe` + private JRE)
