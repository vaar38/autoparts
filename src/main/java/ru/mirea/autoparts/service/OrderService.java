package ru.mirea.autoparts.service;

import ru.mirea.autoparts.exception.BusinessException;
import ru.mirea.autoparts.exception.EntityNotFoundException;
import ru.mirea.autoparts.model.Order;
import ru.mirea.autoparts.model.OrderStatus;
import ru.mirea.autoparts.model.PartCategory;
import ru.mirea.autoparts.repository.CustomerRepository;
import ru.mirea.autoparts.repository.OrderRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Бизнес-логика заказов: создание, изменение, смена статуса, удаление,
 * а также поиск (через SQL), фильтрация и сортировка (через Stream API).
 */
public class OrderService {
    public static final int MAX_QUANTITY = 100;

    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;

    public OrderService(OrderRepository orderRepository, CustomerRepository customerRepository) {
        this.orderRepository = orderRepository;
        this.customerRepository = customerRepository;
    }

    // ------------------------------------------------------------------ CRUD

    public Order create(int customerId, String partName, String partNumber, String carBrand,
                        PartCategory category, int quantity, BigDecimal unitPrice) {
        ensureCustomerExists(customerId);
        validate(partName, partNumber, carBrand, category, quantity, unitPrice);
        Order order = new Order(customerId, partName, partNumber, carBrand, category, quantity, unitPrice);
        orderRepository.save(order);
        return getById(order.getId());   // перечитываем, чтобы получить имя клиента
    }

    public List<Order> getAll() {
        return orderRepository.findAll();
    }

    public Order getById(int id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Заказ", id));
    }

    /** Бизнес-правило: менять состав заказа можно, только пока он в статусе «Новый». */
    public Order update(int id, int customerId, String partName, String partNumber, String carBrand,
                        PartCategory category, int quantity, BigDecimal unitPrice) {
        Order order = getById(id);
        if (order.getStatus() != OrderStatus.NEW) {
            throw new BusinessException("Изменять можно только заказы в статусе «" + OrderStatus.NEW.getTitle()
                    + "». Текущий статус заказа: «" + order.getStatus().getTitle() + "».");
        }
        ensureCustomerExists(customerId);
        validate(partName, partNumber, carBrand, category, quantity, unitPrice);
        order.setCustomerId(customerId);
        order.setPartName(partName);
        order.setPartNumber(partNumber);
        order.setCarBrand(carBrand);
        order.setCategory(category);
        order.setQuantity(quantity);
        order.setUnitPrice(unitPrice);
        orderRepository.update(order);
        return getById(id);
    }

    /** Бизнес-правило: статус меняется только по разрешённым переходам (см. OrderStatus.getAllowedNext). */
    public Order changeStatus(int id, OrderStatus newStatus) {
        Order order = getById(id);
        OrderStatus current = order.getStatus();
        if (!current.canChangeTo(newStatus)) {
            throw new BusinessException("Нельзя перевести заказ из статуса «" + current.getTitle()
                    + "» в статус «" + newStatus.getTitle() + "».");
        }
        order.setStatus(newStatus);
        orderRepository.update(order);
        return order;
    }

    /** Бизнес-правило: удалять можно только новые или отменённые заказы. */
    public void delete(int id) {
        Order order = getById(id);
        if (order.getStatus() != OrderStatus.NEW && order.getStatus() != OrderStatus.CANCELLED) {
            throw new BusinessException("Удалять можно только новые или отменённые заказы. Текущий статус: «"
                    + order.getStatus().getTitle() + "».");
        }
        orderRepository.deleteById(id);
    }

    // ------------------------------------------------------------------ Поиск (SQL LIKE в репозитории)

    public List<Order> searchByPartName(String text) {
        return orderRepository.searchByPartName(requireSearchText(text));
    }

    public List<Order> searchByPartNumber(String text) {
        return orderRepository.searchByPartNumber(requireSearchText(text));
    }

    public List<Order> searchByCarBrand(String text) {
        return orderRepository.searchByCarBrand(requireSearchText(text));
    }

    public List<Order> searchByCustomerName(String text) {
        return orderRepository.searchByCustomerName(requireSearchText(text));
    }

    // ------------------------------------------------------------------ Фильтрация (Stream API)

    public List<Order> filterByStatus(OrderStatus status) {
        return getAll().stream()
                .filter(order -> order.getStatus() == status)
                .toList();
    }

    public List<Order> filterByCategory(PartCategory category) {
        return getAll().stream()
                .filter(order -> order.getCategory() == category)
                .toList();
    }

    public List<Order> filterByCustomer(int customerId) {
        ensureCustomerExists(customerId);
        return getAll().stream()
                .filter(order -> order.getCustomerId() == customerId)
                .toList();
    }

    public List<Order> filterByTotalRange(BigDecimal min, BigDecimal max) {
        if (min.compareTo(max) > 0) {
            throw new BusinessException("Минимальная сумма не может быть больше максимальной.");
        }
        return getAll().stream()
                .filter(order -> order.getTotal().compareTo(min) >= 0 && order.getTotal().compareTo(max) <= 0)
                .toList();
    }

    public List<Order> filterByDateRange(LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw new BusinessException("Начальная дата не может быть позже конечной.");
        }
        return getAll().stream()
                .filter(order -> {
                    LocalDate date = order.getCreatedAt().toLocalDate();
                    return !date.isBefore(from) && !date.isAfter(to);
                })
                .toList();
    }

    // ------------------------------------------------------------------ Сортировка (Stream API + Comparator)

    public List<Order> sort(OrderSort sort) {
        return getAll().stream()
                .sorted(sort.getComparator())
                .toList();
    }

    // ------------------------------------------------------------------ Проверки

    private void ensureCustomerExists(int customerId) {
        if (customerRepository.findById(customerId).isEmpty()) {
            throw new EntityNotFoundException("Клиент", customerId);
        }
    }

    private void validate(String partName, String partNumber, String carBrand,
                          PartCategory category, int quantity, BigDecimal unitPrice) {
        if (partName == null || partName.isBlank()) {
            throw new BusinessException("Название запчасти не может быть пустым.");
        }
        if (partNumber == null || partNumber.isBlank()) {
            throw new BusinessException("Артикул не может быть пустым.");
        }
        if (carBrand == null || carBrand.isBlank()) {
            throw new BusinessException("Марка автомобиля не может быть пустой.");
        }
        if (category == null) {
            throw new BusinessException("Не выбрана категория запчасти.");
        }
        if (quantity < 1 || quantity > MAX_QUANTITY) {
            throw new BusinessException("Количество должно быть от 1 до " + MAX_QUANTITY + " шт.");
        }
        if (unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Цена должна быть больше нуля.");
        }
    }

    private String requireSearchText(String text) {
        if (text == null || text.isBlank()) {
            throw new BusinessException("Строка поиска не может быть пустой.");
        }
        return text.trim();
    }
}
