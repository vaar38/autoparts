package ru.mirea.autoparts.repository;

import ru.mirea.autoparts.model.Order;
import ru.mirea.autoparts.model.OrderStatus;
import ru.mirea.autoparts.model.PartCategory;
import ru.mirea.autoparts.util.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Работа с таблицей orders через JDBC. */
public class OrderRepository implements CrudRepository<Order> {

    // JOIN с таблицей customers, чтобы сразу получить имя клиента для вывода
    private static final String SELECT_BASE =
            "SELECT o.id, o.customer_id, c.full_name AS customer_name, o.part_name, o.part_number, "
            + "o.car_brand, o.category, o.quantity, o.unit_price, o.status, o.created_at "
            + "FROM orders o JOIN customers c ON c.id = o.customer_id ";

    private static final String SELECT_ALL = SELECT_BASE + "ORDER BY o.id";
    private static final String SELECT_BY_ID = SELECT_BASE + "WHERE o.id = ?";
    private static final String SELECT_BY_CUSTOMER = SELECT_BASE + "WHERE o.customer_id = ? ORDER BY o.created_at DESC";

    private static final String INSERT =
            "INSERT INTO orders (customer_id, part_name, part_number, car_brand, category, "
            + "quantity, unit_price, status, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String UPDATE =
            "UPDATE orders SET customer_id = ?, part_name = ?, part_number = ?, car_brand = ?, "
            + "category = ?, quantity = ?, unit_price = ?, status = ? WHERE id = ?";
    private static final String DELETE =
            "DELETE FROM orders WHERE id = ?";
    private static final String COUNT_BY_CUSTOMER =
            "SELECT COUNT(*) FROM orders WHERE customer_id = ?";

    private final DatabaseManager db;

    public OrderRepository(DatabaseManager db) {
        this.db = db;
    }

    @Override
    public Order save(Order order) {
        try (Connection connection = db.getConnection();
             PreparedStatement statement = connection.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {
            fillCommonParameters(statement, order);
            statement.setTimestamp(9, Timestamp.valueOf(order.getCreatedAt()));
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    order.setId(keys.getInt(1));
                }
            }
            return order;
        } catch (SQLException e) {
            throw DatabaseManager.translate(e, "создание заказа");
        }
    }

    @Override
    public List<Order> findAll() {
        return queryList(SELECT_ALL, null, "получение списка заказов");
    }

    @Override
    public Optional<Order> findById(int id) {
        try (Connection connection = db.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_BY_ID)) {
            statement.setInt(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw DatabaseManager.translate(e, "поиск заказа по ID");
        }
    }

    @Override
    public boolean update(Order order) {
        try (Connection connection = db.getConnection();
             PreparedStatement statement = connection.prepareStatement(UPDATE)) {
            fillCommonParameters(statement, order);
            statement.setInt(9, order.getId());
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw DatabaseManager.translate(e, "изменение заказа");
        }
    }

    @Override
    public boolean deleteById(int id) {
        try (Connection connection = db.getConnection();
             PreparedStatement statement = connection.prepareStatement(DELETE)) {
            statement.setInt(1, id);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw DatabaseManager.translate(e, "удаление заказа");
        }
    }

    /** Заказы одного клиента (связанные данные для вкладки «Клиенты»). */
    public List<Order> findByCustomerId(int customerId) {
        return queryList(SELECT_BY_CUSTOMER, customerId, "получение заказов клиента");
    }

    public int countByCustomerId(int customerId) {
        try (Connection connection = db.getConnection();
             PreparedStatement statement = connection.prepareStatement(COUNT_BY_CUSTOMER)) {
            statement.setInt(1, customerId);
            try (ResultSet rs = statement.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw DatabaseManager.translate(e, "подсчёт заказов клиента");
        }
    }

    /** Выполняет SELECT с одним необязательным числовым параметром и собирает список заказов. */
    private List<Order> queryList(String sql, Integer parameter, String action) {
        List<Order> orders = new ArrayList<>();
        try (Connection connection = db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            if (parameter != null) {
                statement.setInt(1, parameter);
            }
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    orders.add(mapRow(rs));
                }
            }
            return orders;
        } catch (SQLException e) {
            throw DatabaseManager.translate(e, action);
        }
    }

    /** Параметры 1–8 одинаковы для INSERT и UPDATE. */
    private void fillCommonParameters(PreparedStatement statement, Order order) throws SQLException {
        statement.setInt(1, order.getCustomerId());
        statement.setString(2, order.getPartName());
        statement.setString(3, order.getPartNumber());
        statement.setString(4, order.getCarBrand());
        statement.setString(5, order.getCategory().name());   // enum -> строка 'BRAKES'
        statement.setInt(6, order.getQuantity());
        statement.setBigDecimal(7, order.getUnitPrice());
        statement.setString(8, order.getStatus().name());
    }

    private Order mapRow(ResultSet rs) throws SQLException {
        return new Order(
                rs.getInt("id"),
                rs.getInt("customer_id"),
                rs.getString("customer_name"),
                rs.getString("part_name"),
                rs.getString("part_number"),
                rs.getString("car_brand"),
                PartCategory.valueOf(rs.getString("category")),   // строка 'BRAKES' -> enum
                rs.getInt("quantity"),
                rs.getBigDecimal("unit_price"),
                OrderStatus.valueOf(rs.getString("status")),
                rs.getTimestamp("created_at").toLocalDateTime());
    }
}
