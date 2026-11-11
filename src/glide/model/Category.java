package glide.model;

/**
 * Fixed enumeration of the 8 student-athlete event categories (SDD Section 4.1 & Table 5-1).
 * Restricts categorization to a predefined whitelist to prevent uncategorized or misspelled entries.
 */
public enum Category {
    EXAM("Exam"),
    LECTURE("Lecture"),
    HOMEWORK("Homework"),
    PRACTICE("Practice"),
    GAME("Game"),
    STUDY("Study"),
    WORK("Work"),
    PERSONAL("Personal");

    // Human-readable label displayed in JComboBox dropdowns and persisted in SQLite
    private final String displayName;

    Category(String name) {
        this.displayName = name;
    }

    /** Returns the formatted category string (e.g., "Practice") for UI rendering and SQL storage. */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Maps a display label from the UI or SQLite row back to its strongly typed Category enum constant.
     * @throws IllegalArgumentException if the label does not match one of the 8 fixed categories.
     */
    public static Category fromDisplayName(String name) {
        if (name != null) {
            // Compare case-insensitively after trimming whitespace
            for (Category c : values()) {
                if (c.displayName.equalsIgnoreCase(name.trim())) {
                    return c;
                }
            }
        }
        throw new IllegalArgumentException("Unknown category: " + name);
    }

    @Override
    public String toString() {
        // Ensures JComboBox<Category> automatically renders the human-readable displayName
        return displayName;
    }
}