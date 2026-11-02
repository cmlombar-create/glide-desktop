package glide.model;

public record Event(long id, String title, String description, int categoryId,
                    String category, String eventDateTime, int reminderLeadMinutes,
                    String reminderDateTime, boolean alertShown) {
    @Override public String toString() { return eventDateTime + " | " + category + " | " + title; }
}
