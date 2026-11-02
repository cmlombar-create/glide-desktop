package glide.model;

public enum Category {
    EXAM("Exam"), LECTURE("Lecture"), HOMEWORK("Homework"),
    PRACTICE("Practice"), GAME("Game"), STUDY("Study"),
    WORK("Work"), PERSONAL("Personal");

    private final String displayName;
    Category(String name) { this.displayName = name; }
    public String getDisplayName() { return displayName; }
    public static Category fromDisplayName(String name) {
        if (name != null) {
            for (Category c : values()) {
                if (c.displayName.equalsIgnoreCase(name.trim())) return c;
            }
        }
        throw new IllegalArgumentException("Unknown category: " + name);
    }
    @Override public String toString() { return displayName; }
}
