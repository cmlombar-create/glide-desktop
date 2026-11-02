package glide.service;

import glide.controller.EventController;

/** Backwards-compatible name; delegates to the SDD AlertService. */
@Deprecated
public class ReminderService extends AlertService {
    public ReminderService() { super(new EventController()); }
}
