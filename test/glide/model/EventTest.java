package glide.model;

import static org.junit.Assert.*;

import java.time.LocalDateTime;
import org.junit.Test;

public class EventTest {
    private static final LocalDateTime FIVE_PM = LocalDateTime.of(2026, 11, 4, 17, 0);

    private Event exam(int leadMinutes) {
        return new Event("Biology Midterm", "", FIVE_PM, Category.EXAM, leadMinutes);
    }

    @Test public void reminderTimeIsEventTimeMinusLeadTime() {
        assertEquals(LocalDateTime.of(2026, 11, 4, 16, 45), exam(15).getReminderTime());
    }

    @Test public void notDueOneSecondBeforeReminderTime() {
        assertFalse(exam(15).isReminderDue(FIVE_PM.minusMinutes(15).minusSeconds(1)));
    }

    @Test public void dueExactlyAtReminderTime() {
        assertTrue(exam(15).isReminderDue(FIVE_PM.minusMinutes(15)));
    }

    @Test public void dueAfterReminderTime() {
        assertTrue(exam(15).isReminderDue(FIVE_PM.minusMinutes(14).plusSeconds(20)));
    }

    @Test public void completedEventIsNeverDue() {
        Event e = exam(15);
        e.setCompleted(true);
        assertFalse(e.isReminderDue(FIVE_PM));
    }

    @Test public void titleAndDescriptionAreTrimmed() {
        Event e = new Event("  Practice  ", "  Field B  ", FIVE_PM, Category.PRACTICE, 15);
        assertEquals("Practice", e.getTitle());
        assertEquals("Field B", e.getDescription());
    }
}
