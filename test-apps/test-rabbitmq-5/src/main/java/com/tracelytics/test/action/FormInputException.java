package com.tracelytics.test.action;


class FormInputException extends Exception {
    private String field;
    public FormInputException(String field, String message) {
        super(message);
        this.field = field;
    }
    
    public FormInputException(String field, String message, Throwable cause) {
        super(message, cause);
        this.field = field;
    }
    
    public String getField() {
        return field;
    }
}
