package ru.mirea.autoparts.util;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Единые форматы вывода денег и дат, чтобы таблицы, статистика и формы выглядели одинаково. */
public final class Formats {
    private static final Locale RUSSIAN = Locale.forLanguageTag("ru-RU");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private Formats() {
    }

    /** 12500.5 -> «12 500,50 руб.» */
    public static String money(BigDecimal value) {
        return value == null ? "" : String.format(RUSSIAN, "%,.2f руб.", value);
    }

    /** 12500.5 -> «12 500,50» — для столбцов таблицы, где единица измерения указана в заголовке. */
    public static String amount(BigDecimal value) {
        return value == null ? "" : String.format(RUSSIAN, "%,.2f", value);
    }

    public static String date(LocalDate value) {
        return value == null ? "" : value.format(DATE);
    }

    public static String dateTime(LocalDateTime value) {
        return value == null ? "" : value.format(DATE_TIME);
    }

    /** Доля в процентах с одним знаком после запятой: 2 из 15 -> «13,3 %». */
    public static String percent(long part, long whole) {
        return whole == 0 ? "0 %" : String.format(RUSSIAN, "%.1f %%", part * 100.0 / whole);
    }
}
