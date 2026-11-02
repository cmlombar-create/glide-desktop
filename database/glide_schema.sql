-- Glide SDD v1: exact seven-column schema
CREATE TABLE IF NOT EXISTS events (
  event_id INTEGER PRIMARY KEY AUTOINCREMENT,
  title TEXT NOT NULL,
  description TEXT NULL,
  date_time TEXT NOT NULL,
  category TEXT NOT NULL,
  alert_lead_time INTEGER NOT NULL DEFAULT 15,
  is_completed INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_active_events ON events (is_completed, date_time);
PRAGMA user_version = 1;
