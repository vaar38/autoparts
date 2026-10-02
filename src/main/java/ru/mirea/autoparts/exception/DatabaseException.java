package ru.mirea.autoparts.exception;

/** Ошибка подключения к базе данных или выполнения SQL-запроса. */
public class DatabaseException extends AppException {
    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }
}
