package ru.mirea.autoparts.exception;

/**
 * Базовое исключение приложения. От него наследуются все наши собственные исключения,
 * поэтому меню может перехватить их все одним catch (AppException e).
 * Наследуется от RuntimeException (непроверяемое исключение), чтобы не писать throws в каждом методе.
 */
public class AppException extends RuntimeException {
    public AppException(String message) {
        super(message);
    }

    public AppException(String message, Throwable cause) {
        super(message, cause);
    }
}
