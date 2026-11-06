package ru.mirea.autoparts.exception;

/**
 * Неверно заполнено конкретное поле формы.
 * Кроме текста ошибки хранит имя поля (field), чтобы окно JavaFX могло подсветить именно это поле.
 * Наследуется от BusinessException, поэтому перехватывается там же, где и остальные ошибки данных.
 */
public class ValidationException extends BusinessException {
    private final String field;

    public ValidationException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
