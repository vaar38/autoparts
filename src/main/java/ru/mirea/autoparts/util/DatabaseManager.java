package ru.mirea.autoparts.util;

import ru.mirea.autoparts.exception.AppException;
import ru.mirea.autoparts.exception.BusinessException;
import ru.mirea.autoparts.exception.DatabaseException;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Отвечает за подключение к PostgreSQL через JDBC.
 * Параметры подключения читаются из файла database.properties в ресурсах приложения
 * (src/main/resources), поэтому пароль не хранится в исходном коде.
 */
public class DatabaseManager {
    private static final String CONFIG_FILE = "database.properties";

    // Коды ошибок SQLState из стандарта SQL
    private static final String UNIQUE_VIOLATION = "23505";
    private static final String FOREIGN_KEY_VIOLATION = "23503";
    private static final String CHECK_VIOLATION = "23514";
    private static final String CONNECTION_ERROR_PREFIX = "08";

    private final String url;
    private final String user;
    private final String password;

    public DatabaseManager() {
        Properties props = new Properties();
        // Ресурс ищется в classpath: Maven копирует его из src/main/resources в target/classes
        InputStream stream = DatabaseManager.class.getClassLoader().getResourceAsStream(CONFIG_FILE);
        if (stream == null) {
            throw new DatabaseException("Не найден файл настроек " + CONFIG_FILE + ".\n"
                    + "Создайте его в папке src/main/resources из образца database.properties.example "
                    + "и впишите свой пароль от PostgreSQL.", null);
        }
        try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            props.load(reader);
        } catch (IOException e) {
            throw new DatabaseException("Не удалось прочитать файл настроек " + CONFIG_FILE + ".", e);
        }
        this.url = props.getProperty("db.url");
        this.user = props.getProperty("db.user");
        this.password = props.getProperty("db.password");
    }

    /** Адрес базы (без пароля) — показывается в заголовке главного окна. */
    public String getUrl() {
        return url;
    }

    /** Открывает новое соединение. Закрывать его нужно через try-with-resources. */
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    /** Проверка при запуске программы: доступна ли база данных. */
    public void checkConnection() {
        try (Connection ignored = getConnection()) {
            // соединение открылось — значит всё в порядке, try-with-resources сразу его закроет
        } catch (SQLException e) {
            throw new DatabaseException("Не удалось подключиться к базе данных " + url + ".\n" + e.getMessage()
                    + "\n\nПроверьте, что PostgreSQL запущен, база autoparts создана, "
                    + "а в database.properties указаны верные логин и пароль.", e);
        }
    }

    /**
     * Превращает SQLException в понятное исключение приложения.
     * Нарушение ограничений таблиц — это ошибка данных (BusinessException),
     * всё остальное — ошибка базы данных (DatabaseException).
     */
    public static AppException translate(SQLException e, String action) {
        String state = e.getSQLState();
        if (UNIQUE_VIOLATION.equals(state)) {
            return new BusinessException("Такая запись уже существует (нарушено ограничение UNIQUE).");
        }
        if (FOREIGN_KEY_VIOLATION.equals(state)) {
            return new BusinessException("Операция нарушает связь между таблицами (FOREIGN KEY).");
        }
        if (CHECK_VIOLATION.equals(state)) {
            return new BusinessException("Недопустимое значение (нарушено ограничение CHECK).");
        }
        if (state != null && state.startsWith(CONNECTION_ERROR_PREFIX)) {
            return new DatabaseException("Нет соединения с базой данных: " + e.getMessage(), e);
        }
        return new DatabaseException("Ошибка SQL-запроса (" + action + "): " + e.getMessage(), e);
    }

    public static boolean isUniqueViolation(SQLException e) {
        return UNIQUE_VIOLATION.equals(e.getSQLState());
    }
}
