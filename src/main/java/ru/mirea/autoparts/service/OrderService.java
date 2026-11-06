package ru.mirea.autoparts.service;

import ru.mirea.autoparts.exception.BusinessException;
import ru.mirea.autoparts.exception.EntityNotFoundException;
import ru.mirea.autoparts.exception.ValidationException;
import ru.mirea.autoparts.model.Order;
import ru.mirea.autoparts.model.OrderStatus;
import ru.mirea.autoparts.model.PartCategory;
import ru.mirea.autoparts.repository.CustomerRepository;
import ru.mirea.autoparts.repository.OrderRepository;

import java.math.BigDecimal;
import java.util.List;

/**
 * Бизнес-логика заказов: создание, изменение, смена статуса, удаление и проверка данных.
 * Поиск и фильтрация описаны в OrderFilter, сортировка — в OrderSort.
 */
public class OrderService {
    public static final int MAX_QUANTITY = 100;
    /** Верхняя граница цены: NUMERIC(10,2) в таблице вмещает до 99 999 999.99. */
    public static final BigDecimal MAX_PRICE = new BigDecimal("99999999.99");

    // Имена полей формы — по ним окно подсвечивает поле с ошибкой
    public static final String FIELD_CUSTOMER = "customer";
    public static final String FIELD_PART_NAME = "partName";
    public static final String FIELD_PART_NUMBER = "partNumber";
    public static final String FIELD_CAR_BRAND = "carBrand";
    public static final String FIELD_CATEGORY = "category";
    public static final String FIELD_QUANTITY = "quantity";
    public static final String FIELD_UNIT_PRICE = "unitPrice";

    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;

    public OrderService(OrderRepository orderRepository, CustomerRepository customerRepository) {
        this.orderRepository = orderRepository;
        this.customerRepository = customerRepository;
    }

    // ------------------------------------------------------------------ CRUD

    public Order create(int customerId, String partName, String partNumber, String carBrand,
                        PartCategory category, int quantity, BigDecimal unitPrice) {
        Order order = new Order(customerId, partName, partNumber, carBrand, category, quantity, unitPrice);
        validate(order);
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

    /** Заказы одного клиента (связанные данные). */
    public List<Order> getByCustomer(int customerId) {
        return orderRepository.findByCustomerId(customerId);
    }

    /** Бизнес-правило: менять состав заказа можно, только пока он в статусе «Новый». */
    public Order update(int id, int customerId, String partName, String partNumber, String carBrand,
                        PartCategory category, int quantity, BigDecimal unitPrice) {
        Order order = getById(id);
        ensureEditable(order);
        order.setCustomerId(customerId);
        order.setPartName(partName);
        order.setPartNumber(partNumber);
        order.setCarBrand(carBrand);
        order.setCategory(category);
        order.setQuantity(quantity);
        order.setUnitPrice(unitPrice);
        validate(order);
        orderRepository.update(order);
        return getById(id);
    }

    /** Проверка перед открытием формы редактирования: не даём править заказ, который уже нельзя менять. */
    public void ensureEditable(Order order) {
        if (order.getStatus() != OrderStatus.NEW) {
            throw new BusinessException("Изменять можно только заказы в статусе «" + OrderStatus.NEW.getTitle()
                    + "». Текущий статус заказа №" + order.getId() + ": «" + order.getStatus().getTitle() + "».");
        }
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
                    + order.getStatus().getTitle() + "». Сначала отмените заказ или дождитесь его завершения.");
        }
        orderRepository.deleteById(id);
    }

    // ------------------------------------------------------------------ Проверки

    /** Проверяет все поля заказа до сохранения. Строки очищаются от пробелов по краям. */
    private void validate(Order order) {
        if (order.getCustomerId() <= 0) {
            throw new ValidationException(FIELD_CUSTOMER, "Выберите клиента из списка.");
        }
        // Ссылка на связанную сущность: клиент должен существовать в базе
        if (customerRepository.findById(order.getCustomerId()).isEmpty()) {
            throw new ValidationException(FIELD_CUSTOMER, "Выбранный клиент не найден в базе — обновите список клиентов.");
        }
        order.setPartName(Validation.requireText(order.getPartName(), FIELD_PART_NAME, "Название запчасти", 100));
        order.setPartNumber(Validation.requireText(order.getPartNumber(), FIELD_PART_NUMBER, "Артикул", 30));
        if (!order.getPartNumber().matches("[A-Za-z0-9-]+")) {
            throw new ValidationException(FIELD_PART_NUMBER, "Артикул может содержать только латинские буквы, цифры и дефис.");
        }
        order.setCarBrand(Validation.requireText(order.getCarBrand(), FIELD_CAR_BRAND, "Марка автомобиля", 50));
        if (order.getCategory() == null) {
            throw new ValidationException(FIELD_CATEGORY, "Выберите категорию запчасти.");
        }
        if (order.getQuantity() < 1 || order.getQuantity() > MAX_QUANTITY) {
            throw new ValidationException(FIELD_QUANTITY, "Количество должно быть от 1 до " + MAX_QUANTITY + " шт.");
        }
        BigDecimal price = order.getUnitPrice();
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException(FIELD_UNIT_PRICE, "Цена должна быть больше нуля.");
        }
        if (price.compareTo(MAX_PRICE) > 0) {
            throw new ValidationException(FIELD_UNIT_PRICE, "Цена слишком большая: максимум 99 999 999,99 руб.");
        }
    }
}
