package ru.mirea.autoparts.util;

import ru.mirea.autoparts.exception.AppException;
import ru.mirea.autoparts.exception.BusinessException;
import ru.mirea.autoparts.exception.DatabaseException;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Отвечает за подключение к PostgreSQL через JDBC.
 * Параметры подключения читаются из файла db.properties в папке проекта.
 */
public class DatabaseManager {
    private static final String CONFIG_FILE = "db.properties";

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
        try (Reader reader = Files.newBufferedReader(Path.of(CONFIG_FILE), StandardCharsets.UTF_8)) {
            props.load(reader);
        } catch (IOException e) {
            throw new DatabaseException("Не удалось прочитать файл настроек " + CONFIG_FILE
                    + ". Он должен лежать в папке проекта рядом с pom.xml. "
                    + "Создайте его из образца db.properties.example и впишите свой пароль.", e);
        }
        this.url = props.getProperty("db.url");
        this.user = props.getProperty("db.user");
        this.password = props.getProperty("db.password");
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
            throw new DatabaseException("Не удалось подключиться к базе данных " + url + ": " + e.getMessage(), e);
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
