package glide.service;

import static org.junit.Assert.*;

import glide.controller.EventController;
import glide.dao.InMemoryEventDAO;
import glide.model.Category;
import glide.model.Event;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

public class AlertServiceTest {

    /** Records popups instead of drawing them, so tests run without a screen. */
    static class RecordingAlertService extends AlertService {
        final List<String> shown = new ArrayList<>();
        Runnable duringPopup;
        RecordingAlertService(EventController controller) { super(controller); }
        @Override public void showReminderPopup(Event event, LocalDateTime now) {
            shown.add(buildReminderMessage(event, now));
            if (duringPopup != null) duringPopup.run();
        }
    }

    private static final LocalDateTime FIVE_PM = LocalDateTime.of(2026, 11, 4, 17, 0);

    private InMemoryEventDAO dao;
    private RecordingAlertService alerts;

    @Before public void setUp() {
        dao = new InMemoryEventDAO();
        alerts = new RecordingAlertService(new EventController(dao));
    }

    /** Puts an event straight into the fake database (skips the "must be in the future" check). */
    private int save(String title, String details, LocalDateTime when, Category category, int lead) {
        return dao.insertEvent(new Event(title, details, when, category, lead));
    }

    @Test public void noPopupBeforeTheReminderTime() {
        save("Biology Midterm", "", FIVE_PM, Category.EXAM, 15);
        alerts.checkAt(FIVE_PM.minusMinutes(15).minusSeconds(10)); // 4:44:50
        assertTrue(alerts.shown.isEmpty());
    }

    @Test public void popupAppearsOnceWhenDue() {
        int id = save("Biology Midterm", "Room 202, bring calculator", FIVE_PM, Category.EXAM, 15);
        alerts.checkAt(FIVE_PM.minusMinutes(15).plusSeconds(20)); // 4:45:20
        assertEquals(1, alerts.shown.size());
        assertTrue(dao.rows.get(id).isCompleted());

        alerts.checkAt(FIVE_PM.minusMinutes(14)); // next check
        assertEquals("no duplicate popup", 1, alerts.shown.size());
    }

    @Test public void popupTextMatchesTheDesign() {
        save("Biology Midterm", "Room 202, bring calculator", FIVE_PM, Category.EXAM, 15);
        alerts.checkAt(FIVE_PM.minusMinutes(15).plusSeconds(20));
        assertEquals("Biology Midterm\n"
                + "Starts in 15 minutes\n"
                + "When: Wed, Nov 4, 2026 at 5:00 PM\n"
                + "Category: Exam\n"
                + "Details: Room 202, bring calculator", alerts.shown.get(0));
    }

    @Test public void detailsLineIsLeftOutWhenEmpty() {
        save("Shift", "", FIVE_PM, Category.WORK, 15);
        alerts.checkAt(FIVE_PM.minusMinutes(15));
        assertFalse(alerts.shown.get(0).contains("Details:"));
    }

    @Test public void zeroLeadTimeSaysStartingNow() {
        save("Shift", "", FIVE_PM, Category.WORK, 0);
        alerts.checkAt(FIVE_PM.plusSeconds(25));
        assertTrue(alerts.shown.get(0).contains("Starting now"));
    }

    @Test public void missedReminderIsSkippedButMarked() {
        int id = save("Lecture", "", FIVE_PM, Category.LECTURE, 15);
        alerts.checkAt(FIVE_PM.plusHours(3)); // app reopened 3 hours later
        assertTrue(alerts.shown.isEmpty());
        assertTrue(dao.rows.get(id).isCompleted());
    }

    @Test public void lateReminderStillShowsIfEventIsUpcoming() {
        save("Game", "", FIVE_PM, Category.GAME, 30);
        alerts.checkAt(FIVE_PM.minusMinutes(10)); // opened after 4:30, before 5:00
        assertEquals(1, alerts.shown.size());
        assertTrue(alerts.shown.get(0).contains("Starts in 10 minutes"));
    }

    @Test public void secondCheckDuringAnOpenPopupIsIgnored() {
        save("Study A", "", FIVE_PM, Category.STUDY, 15);
        save("Study B", "", FIVE_PM.plusMinutes(1), Category.STUDY, 15);
        LocalDateTime now = FIVE_PM.minusMinutes(13);
        alerts.duringPopup = () -> alerts.checkAt(now); // timer fires while the dialog is open
        alerts.checkAt(now);
        assertEquals("each reminder shown exactly once", 2, alerts.shown.size());
    }

    @Test public void databaseErrorDoesNotCrashOrShowPopups() {
        save("Exam", "", FIVE_PM, Category.EXAM, 15);
        dao.failReads = true;
        alerts.checkAt(FIVE_PM);
        assertTrue(alerts.shown.isEmpty());
        dao.failReads = false;
        alerts.checkAt(FIVE_PM.minusMinutes(1)); // next check retries and works
        assertEquals(1, alerts.shown.size());
    }

    @Test public void notRunningMeansNoChecks() {
        save("Exam", "", LocalDateTime.now().minusMinutes(1), Category.EXAM, 0);
        alerts.checkPendingReminders(); // start() was never called
        assertTrue(alerts.shown.isEmpty());
    }

    @Test public void windowTitleIncludesTheCategory() {
        Event e = new Event("Biology Midterm", "", FIVE_PM, Category.EXAM, 15);
        assertEquals("Glide Reminder - Exam", AlertService.buildReminderTitle(e));
    }

    @Test public void durationsReadNaturally() {
        assertEquals("1 minute", AlertService.formatDuration(1));
        assertEquals("15 minutes", AlertService.formatDuration(15));
        assertEquals("1 hour 30 minutes", AlertService.formatDuration(90));
        assertEquals("2 hours", AlertService.formatDuration(120));
        assertEquals("1 day", AlertService.formatDuration(1440));
    }
}
