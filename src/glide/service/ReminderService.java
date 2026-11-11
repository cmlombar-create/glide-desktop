package glide.service;

import glide.controller.EventController;

/**
 * Legacy compatibility alias that delegates directly to AlertService (SDD Section 4.4).
 */
@Deprecated
public class ReminderService extends AlertService {
    public ReminderService() {
        super(new EventController());
    }
}