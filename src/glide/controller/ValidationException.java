package glide.controller;

import java.util.List;

/**
 * Checked exception thrown when user form input violates business validation rules (SDD Section 4.6).
 * Carries the complete list of human-readable error messages back to EventDialog.
 */
public class ValidationException extends Exception {
    private final List<String> errors;

    public ValidationException(List<String> errors) {
        super(String.join("\n", errors));
        this.errors = List.copyOf(errors);
    }

    /** Returns all accumulated validation error messages for bulleted display in the UI. */
    public List<String> getErrors() {
        return errors;
    }
}