package org.example.productshop.exception;

public class HolidayClosedException extends RuntimeException {
    public HolidayClosedException() { super("休市"); }
}