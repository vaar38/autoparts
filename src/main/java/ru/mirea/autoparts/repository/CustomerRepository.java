package ru.mirea.autoparts.repository;

import ru.mirea.autoparts.exception.AppException;
import ru.mirea.autoparts.exception.BusinessException;
import ru.mirea.autoparts.model.Customer;
import ru.mirea.autoparts.util.DatabaseManager;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Работа с таблицей customers через JDBC. */
public class CustomerRepository implements CrudRepository<Customer> {

    // Все запросы параметризованы: вместо значений стоят знаки ?, значения подставляет PreparedStatement.
    private static final String INSERT =
            "INSERT INTO customers (full_name, phone, email, city, registered_at) VALUES (?, ?, ?, ?, ?)";
    private static final String SELECT_ALL =
            "SELECT id, full_name, phone, email, city, registered_at FROM customers ORDER BY id";
    private static final String SELECT_BY_ID =
            "SELECT id, full_name, phone, email, city, registered_at FROM customers WHERE id = ?";
    private static final String UPDATE =
            "UPDATE customers SET full_name = ?, phone = ?, email = ?, city = ?, registered_at = ? WHERE id = ?";
    private static final String DELETE =
            "DELETE FROM customers WHERE id = ?";
    private static final String COUNT =
            "SELECT COUNT(*) FROM customers";
    // id <> ? — при редактировании сам клиент не считается «дубликатом» самого себя
    private static final String EXISTS_PHONE =
            "SELECT EXISTS (SELECT 1 FROM customers WHERE phone = ? AND id <> ?)";
    private static final String EXISTS_EMAIL =
            "SELECT EXISTS (SELECT 1 FROM customers WHERE LOWER(email) = LOWER(?) AND id <> ?)";

    private final DatabaseManager db;

    public CustomerRepository(DatabaseManager db) {
        this.db = db;
    }

    @Override
    public Customer save(Customer customer) {
        try (Connection connection = db.getConnection();
             PreparedStatement statement = connection.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, customer.getFullName());
            statement.setString(2, customer.getPhone());
            statement.setString(3, customer.getEmail());
            statement.setString(4, customer.getCity());
            statement.setDate(5, Date.valueOf(customer.getRegisteredAt()));
            statement.executeUpdate();

            // Узнаём id, который база данных присвоила новой записи (SERIAL)
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    customer.setId(keys.getInt(1));
                }
            }
            return customer;
        } catch (SQLException e) {
            throw handle(e, "добавление клиента");
        }
    }

    @Override
    public List<Customer> findAll() {
        List<Customer> customers = new ArrayList<>();
        try (Connection connection = db.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_ALL);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                customers.add(mapRow(rs));
            }
            return customers;
        } catch (SQLException e) {
            throw handle(e, "получение списка клиентов");
        }
    }

    @Override
    public Optional<Customer> findById(int id) {
        try (Connection connection = db.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_BY_ID)) {
            statement.setInt(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw handle(e, "поиск клиента по ID");
        }
    }

    @Override
    public boolean update(Customer customer) {
        try (Connection connection = db.getConnection();
             PreparedStatement statement = connection.prepareStatement(UPDATE)) {
            statement.setString(1, customer.getFullName());
            statement.setString(2, customer.getPhone());
            statement.setString(3, customer.getEmail());
            statement.setString(4, customer.getCity());
            statement.setDate(5, Date.valueOf(customer.getRegisteredAt()));
            statement.setInt(6, customer.getId());
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw handle(e, "изменение клиента");
        }
    }

    @Override
    public boolean deleteById(int id) {
        try (Connection connection = db.getConnection();
             PreparedStatement statement = connection.prepareStatement(DELETE)) {
            statement.setInt(1, id);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw handle(e, "удаление клиента");
        }
    }

    /** Количество клиентов — агрегатная функция COUNT(*) считается на стороне базы. */
    public int count() {
        try (Connection connection = db.getConnection();
             PreparedStatement statement = connection.prepareStatement(COUNT);
             ResultSet rs = statement.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        } catch (SQLException e) {
            throw handle(e, "подсчёт клиентов");
        }
    }

    /** Есть ли другой клиент (с id, не равным excludeId) с таким телефоном. */
    public boolean existsByPhone(String phone, int excludeId) {
        return exists(EXISTS_PHONE, phone, excludeId, "проверка телефона");
    }

    /** Есть ли другой клиент (с id, не равным excludeId) с таким email. */
    public boolean existsByEmail(String email, int excludeId) {
        return exists(EXISTS_EMAIL, email, excludeId, "проверка email");
    }

    private boolean exists(String sql, String value, int excludeId, String action) {
        try (Connection connection = db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, value);
            statement.setInt(2, excludeId);
            try (ResultSet rs = statement.executeQuery()) {
                rs.next();
                return rs.getBoolean(1);
            }
        } catch (SQLException e) {
            throw handle(e, action);
        }
    }

    /** Превращает одну строку результата SELECT в объект Customer. */
    private Customer mapRow(ResultSet rs) throws SQLException {
        return new Customer(
                rs.getInt("id"),
                rs.getString("full_name"),
                rs.getString("phone"),
                rs.getString("email"),
                rs.getString("city"),
                rs.getDate("registered_at").toLocalDate());
    }

    private AppException handle(SQLException e, String action) {
        if (DatabaseManager.isUniqueViolation(e)) {
            return new BusinessException("Клиент с таким телефоном или email уже существует.");
        }
        return DatabaseManager.translate(e, action);
    }
}
