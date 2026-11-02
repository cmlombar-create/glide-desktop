-- Original Glide CSI 2999 SQLite schema
PRAGMA foreign_keys = ON;
CREATE TABLE IF NOT EXISTS categories (
    category_id INTEGER PRIMARY KEY,
    name TEXT NOT NULL UNIQUE,
    display_order INTEGER NOT NULL UNIQUE CHECK (display_order > 0)
);
CREATE TABLE IF NOT EXISTS events (
    event_id INTEGER PRIMARY KEY AUTOINCREMENT,
    title TEXT NOT NULL CHECK (length(trim(title)) > 0),
    description TEXT NOT NULL DEFAULT '',
    category_id INTEGER NOT NULL,
    event_datetime TEXT NOT NULL,
    reminder_lead_minutes INTEGER NOT NULL DEFAULT 15 CHECK (reminder_lead_minutes >= 0),
    reminder_datetime TEXT NOT NULL,
    alert_shown INTEGER NOT NULL DEFAULT 0 CHECK (alert_shown IN (0,1)),
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (category_id) REFERENCES categories(category_id)
       ON UPDATE CASCADE ON DELETE RESTRICT,
    CHECK (datetime(reminder_datetime) <= datetime(event_datetime))
);
CREATE INDEX IF NOT EXISTS idx_events_event_datetime ON events(event_datetime);
CREATE INDEX IF NOT EXISTS idx_events_reminder_due ON events(alert_shown, reminder_datetime);
CREATE INDEX IF NOT EXISTS idx_events_category_datetime ON events(category_id, event_datetime);
INSERT OR IGNORE INTO categories (category_id,name,display_order) VALUES
(1,'Exam',1),(2,'Lecture',2),(3,'Homework',3),(4,'Practice',4),
(5,'Game',5),(6,'Study',6),(7,'Work',7),(8,'Personal',8);
