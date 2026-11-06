package ru.mirea.autoparts.service;

import ru.mirea.autoparts.exception.ValidationException;

import java.math.BigDecimal;

/**
 * Общие проверки пользовательского ввода. Используются сервисами (обязательность, длина строк)
 * и окнами-формами (разбор чисел из текстовых полей). Все методы бросают ValidationException
 * с именем поля и понятным сообщением: что заполнено неверно и как исправить.
 */
public final class Validation {

    private Validation() {
        // утилитарный класс: только статические методы, объекты не создаём
    }

    /** Обязательная строка с ограничением длины (длина совпадает с VARCHAR(n) в таблице). Возвращает строку без пробелов по краям. */
    public static String requireText(String value, String field, String label, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new ValidationException(field, "Поле «" + label + "» обязательно для заполнения.");
        }
        String trimmed = value.trim();
        if (trimmed.length() > maxLength) {
            throw new ValidationException(field, "Поле «" + label + "» слишком длинное: " + trimmed.length()
                    + " символов, допускается не более " + maxLength + ".");
        }
        return trimmed;
    }

    /** Целое число в диапазоне [min, max] из текстового поля. */
    public static int parseInt(String text, String field, String label, int min, int max) {
        if (text == null || text.isBlank()) {
            throw new ValidationException(field, "Поле «" + label + "» обязательно для заполнения.");
        }
        int value;
        try {
            value = Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            throw new ValidationException(field, "Поле «" + label + "» должно быть целым числом, например 2.");
        }
        if (value < min || value > max) {
            throw new ValidationException(field, "Поле «" + label + "» должно быть от " + min + " до " + max + ".");
        }
        return value;
    }

    /** Денежная сумма из текстового поля: допускается запятая вместо точки, не более 2 знаков после запятой. */
    public static BigDecimal parseMoney(String text, String field, String label) {
        if (text == null || text.isBlank()) {
            throw new ValidationException(field, "Поле «" + label + "» обязательно для заполнения.");
        }
        BigDecimal value;
        try {
            value = new BigDecimal(text.trim().replace(" ", "").replace(',', '.'));
        } catch (NumberFormatException e) {
            throw new ValidationException(field, "Поле «" + label + "» должно быть числом, например 3450.00.");
        }
        if (value.scale() > 2) {
            throw new ValidationException(field, "В поле «" + label + "» допускается не более 2 знаков после запятой (копейки).");
        }
        return value;
    }
}
