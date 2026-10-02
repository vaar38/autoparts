package ru.mirea.autoparts.ui;

import java.io.Console;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Scanner;
import java.util.function.Function;

/**
 * Чтение данных с клавиатуры с проверкой. При неверном вводе выводится ошибка
 * и значение запрашивается снова — программа не падает.
 */
public class ConsoleInput {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final Scanner scanner = new Scanner(System.in, detectConsoleCharset());

    /** Читает строку (может быть пустой). */
    public String readLine(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }

    /** Читает непустую строку. */
    public String readRequired(String prompt) {
        while (true) {
            String value = readLine(prompt);
            if (!value.isEmpty()) {
                return value;
            }
            printError("значение не может быть пустым.");
        }
    }

    /** Для редактирования: пустой ввод (Enter) оставляет текущее значение. */
    public String readOrKeep(String prompt, String current) {
        String value = readLine(prompt + " [" + current + "]: ");
        return value.isEmpty() ? current : value;
    }

    public int readInt(String prompt, String errorMessage) {
        while (true) {
            String value = readLine(prompt);
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                printError(errorMessage);
            }
        }
    }

    public int readId(String prompt) {
        return readInt(prompt, "ID должен быть целым числом.");
    }

    public int readIntOrKeep(String prompt, int current) {
        while (true) {
            String value = readLine(prompt + " [" + current + "]: ");
            if (value.isEmpty()) {
                return current;
            }
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                printError("нужно ввести целое число.");
            }
        }
    }

    public BigDecimal readDecimal(String prompt) {
        while (true) {
            BigDecimal value = parseDecimal(readLine(prompt));
            if (value != null) {
                return value;
            }
            printError("нужно ввести число, например 1500 или 1499.90.");
        }
    }

    public BigDecimal readDecimalOrKeep(String prompt, BigDecimal current) {
        while (true) {
            String text = readLine(prompt + " [" + current.toPlainString() + "]: ");
            if (text.isEmpty()) {
                return current;
            }
            BigDecimal value = parseDecimal(text);
            if (value != null) {
                return value;
            }
            printError("нужно ввести число, например 1500 или 1499.90.");
        }
    }

    public LocalDate readDate(String prompt) {
        while (true) {
            String value = readLine(prompt);
            try {
                return LocalDate.parse(value, DATE_FORMAT);
            } catch (DateTimeParseException e) {
                printError("дата должна быть в формате ДД.ММ.ГГГГ, например 15.09.2026.");
            }
        }
    }

    /**
     * Выбор одного значения из списка (например, статуса или категории).
     * Метод обобщённый (generic): работает с любым типом T.
     */
    public <T> T choose(String header, List<T> options, Function<T, String> titleOf) {
        System.out.println(header);
        for (int i = 0; i < options.size(); i++) {
            System.out.println("  " + (i + 1) + ". " + titleOf.apply(options.get(i)));
        }
        while (true) {
            int number = readInt("Ваш выбор: ", "введите номер из списка.");
            if (number >= 1 && number <= options.size()) {
                return options.get(number - 1);
            }
            printError("введите номер от 1 до " + options.size() + ".");
        }
    }

    public boolean confirm(String prompt) {
        String answer = readLine(prompt + " (да/нет): ").toLowerCase();
        return answer.equals("да") || answer.equals("д") || answer.equals("y") || answer.equals("yes");
    }

    public static void printError(String message) {
        System.out.println("Ошибка: " + message);
    }

    private BigDecimal parseDecimal(String text) {
        try {
            return new BigDecimal(text.replace(',', '.'));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Кодировка консоли Windows (обычно cp866), чтобы русские буквы читались правильно. */
    private static Charset detectConsoleCharset() {
        String stdinEncoding = System.getProperty("stdin.encoding");
        if (stdinEncoding != null) {
            try {
                return Charset.forName(stdinEncoding);
            } catch (RuntimeException ignored) {
                // неизвестная кодировка — пробуем следующий способ
            }
        }
        Console console = System.console();
        return console != null ? console.charset() : Charset.defaultCharset();
    }
}
