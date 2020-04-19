package ru.ifmo.rain.korobkov.walk;

public class WalkerException extends Exception {
    public WalkerException(final String error, final Exception e) {
        super(error + e.getMessage(), e);
    }
    public WalkerException(final String error) {
        super(error);
    }
}
