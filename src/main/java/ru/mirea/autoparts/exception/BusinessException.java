package ru.mirea.autoparts.exception;

/** Нарушено бизнес-правило (неверные данные или недопустимая операция). */
public class BusinessException extends AppException {
    public BusinessException(String message) {
        super(message);
    }
}
