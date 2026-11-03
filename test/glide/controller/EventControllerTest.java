package glide.controller;

import static org.junit.Assert.*;

import glide.dao.DatabaseException;
import glide.dao.InMemoryEventDAO;
import glide.model.Category;
import glide.model.Event;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

public class EventControllerTest {
    private InMemoryEventDAO dao;
    private EventController controller;
    private LocalDateTime tomorrow;

    @Before public void setUp() {
        dao = new InMemoryEventDAO();
        controller = new EventController(dao);
        tomorrow = LocalDateTime.now().plusDays(1).withSecond(0).withNano(0);
    }

    // validateInputs

    @Test public void validInputHasNoErrors() {
        assertTrue(controller.validateInputs("Bio exam", "", tomorrow, Category.EXAM, 15).isEmpty());
    }

    @Test public void blankOrMissingTitleIsRejected() {
        assertTrue(controller.validateInputs("   ", "", tomorrow, Category.EXAM, 15).contains("Title is required."));
        assertTrue(controller.validateInputs(null, "", tomorrow, Category.EXAM, 15).contains("Title is required."));
    }

    @Test public void titleLengthLimitIs100() {
        assertTrue(controller.validateInputs("x".repeat(100), "", tomorrow, Category.EXAM, 15).isEmpty());
        assertTrue(controller.validateInputs("x".repeat(101), "", tomorrow, Category.EXAM, 15)
                .contains("Title must be 100 characters or fewer."));
    }

    @Test public void descriptionLengthLimitIs500() {
        assertTrue(controller.validateInputs("t", "d".repeat(500), tomorrow, Category.EXAM, 15).isEmpty());
        assertTrue(controller.validateInputs("t", "d".repeat(501), tomorrow, Category.EXAM, 15)
                .contains("Description must be 500 characters or fewer."));
    }

    @Test public void pastOrMissingDateIsRejected() {
        assertTrue(controller.validateInputs("t", "", LocalDateTime.now().minusMinutes(1), Category.EXAM, 15)
                .contains("Event date and time must be in the future."));
        assertTrue(controller.validateInputs("t", "", null, Category.EXAM, 15)
                .contains("Date and time are required."));
    }

    @Test public void categoryIsRequired() {
        assertTrue(controller.validateInputs("t", "", tomorrow, null, 15).contains("Please choose a category."));
    }

    @Test public void reminderMustBeOneOfTheOptions() {
        for (int ok : new int[]{0, 5, 10, 15, 30, 60, 120, 1440}) {
            assertTrue("lead " + ok, controller.validateInputs("t", "", tomorrow, Category.EXAM, ok).isEmpty());
        }
        assertTrue(controller.validateInputs("t", "", tomorrow, Category.EXAM, 7)
                .contains("Please choose a reminder time from the list."));
        assertTrue(controller.validateInputs("t", "", tomorrow, Category.EXAM, -5)
                .contains("Please choose a reminder time from the list."));
    }

    @Test public void allErrorsAreReturnedTogether() {
        assertEquals(4, controller.validateInputs("", "", null, null, 7).size());
    }

    // addEvent

    @Test public void addEventSavesAndReturnsNewId() throws Exception {
        int id = controller.addEvent("  Practice  ", null, tomorrow, Category.PRACTICE, 15);
        assertEquals(1, id);
        assertEquals("Practice", dao.rows.get(1).getTitle());
        assertFalse(dao.rows.get(1).isCompleted());
    }

    @Test public void invalidEventIsNotSaved() {
        try {
            controller.addEvent("", "", LocalDateTime.now().minusDays(1), Category.EXAM, 15);
            fail("expected ValidationException");
        } catch (ValidationException ex) {
            assertEquals(2, ex.getErrors().size());
        }
        assertTrue(dao.rows.isEmpty());
    }

    // updateEvent: timing changes reset isCompleted, other changes keep it

    private Event savedCompletedEvent() throws Exception {
        int id = controller.addEvent("Practice", "", tomorrow, Category.PRACTICE, 15);
        dao.markCompleted(id); // the reminder has already fired
        return controller.findEventById(id);
    }

    @Test public void changingTheTimeReArmsTheReminder() throws Exception {
        Event e = savedCompletedEvent();
        e.setDateTime(tomorrow.plusHours(2));
        controller.updateEvent(e);
        assertFalse(dao.rows.get(e.getId()).isCompleted());
    }

    @Test public void changingTheLeadTimeReArmsTheReminder() throws Exception {
        Event e = savedCompletedEvent();
        e.setAlertLeadTime(30);
        controller.updateEvent(e);
        assertFalse(dao.rows.get(e.getId()).isCompleted());
    }

    @Test public void changingOnlyTheWordsKeepsTheReminderStatus() throws Exception {
        Event e = savedCompletedEvent();
        e.setTitle("Practice (Field B)");
        e.setCategory(Category.GAME);
        e.setCompleted(false); // ignored: the stored isCompleted value is kept
        controller.updateEvent(e);
        assertTrue(dao.rows.get(e.getId()).isCompleted());
        assertEquals("Practice (Field B)", dao.rows.get(e.getId()).getTitle());
    }

    @Test public void updatingADeletedEventThrowsNotFound() {
        Event ghost = new Event(99, "Ghost", "", tomorrow, Category.WORK, 15, false);
        try {
            controller.updateEvent(ghost);
            fail("expected DatabaseException");
        } catch (DatabaseException ex) {
            assertEquals("This event no longer exists.", ex.getMessage());
        } catch (ValidationException ex) {
            fail("input was valid");
        }
    }

    // deleteEvent

    @Test public void deleteRemovesTheEvent() throws Exception {
        int id = controller.addEvent("Shift", "", tomorrow, Category.WORK, 0);
        assertTrue(controller.deleteEvent(id));
        assertNull(dao.rows.get(id));
    }

    @Test public void deletingTwiceThrowsNotFound() throws Exception {
        int id = controller.addEvent("Shift", "", tomorrow, Category.WORK, 0);
        controller.deleteEvent(id);
        try {
            controller.deleteEvent(id);
            fail("expected DatabaseException");
        } catch (DatabaseException ex) {
            assertEquals("This event no longer exists.", ex.getMessage());
        }
    }

    // getEvents and getDueReminders

    @Test public void getEventsFiltersByCategoryInDateOrder() throws Exception {
        controller.addEvent("Study B", "", tomorrow.plusHours(2), Category.STUDY, 15);
        controller.addEvent("Game", "", tomorrow, Category.GAME, 15);
        controller.addEvent("Study A", "", tomorrow.plusHours(1), Category.STUDY, 15);
        List<Event> study = controller.getEvents(Category.STUDY);
        assertEquals(2, study.size());
        assertEquals("Study A", study.get(0).getTitle());
        assertEquals(3, controller.getEvents(null).size());
    }

    @Test public void getDueRemindersReturnsOnlyDueAndNotCompleted() throws Exception {
        int soon = controller.addEvent("Soon", "", tomorrow, Category.EXAM, 15);
        controller.addEvent("Later", "", tomorrow.plusDays(1), Category.EXAM, 15);
        LocalDateTime atReminder = tomorrow.minusMinutes(15);
        assertEquals(1, controller.getDueReminders(atReminder).size());
        controller.markCompleted(soon);
        assertTrue(controller.getDueReminders(atReminder).isEmpty());
    }
}
