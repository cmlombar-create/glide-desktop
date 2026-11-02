package glide.controller;

import java.util.List;

public class ValidationException extends Exception {
    private final List<String> errors;
    public ValidationException(List<String> errors) {
        super(String.join("\n", errors));
        this.errors = List.copyOf(errors);
    }
    public List<String> getErrors() { return errors; }
}
