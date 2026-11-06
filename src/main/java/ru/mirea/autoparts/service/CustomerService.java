package ru.mirea.autoparts.service;

import ru.mirea.autoparts.exception.BusinessException;
import ru.mirea.autoparts.exception.EntityNotFoundException;
import ru.mirea.autoparts.exception.ValidationException;
import ru.mirea.autoparts.model.Customer;
import ru.mirea.autoparts.repository.CustomerRepository;
import ru.mirea.autoparts.repository.OrderRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

/** Бизнес-логика для клиентов: проверки данных и правила перед обращением к базе. */
public class CustomerService {
    // Имена полей формы — по ним окно подсвечивает поле с ошибкой
    public static final String FIELD_FULL_NAME = "fullName";
    public static final String FIELD_PHONE = "phone";
    public static final String FIELD_EMAIL = "email";
    public static final String FIELD_CITY = "city";
    public static final String FIELD_REGISTERED_AT = "registeredAt";

    /** Магазин открылся в 2000 году — более ранняя дата регистрации считается ошибкой ввода. */
    public static final LocalDate MIN_REGISTRATION_DATE = LocalDate.of(2000, 1, 1);

    private static final String PHONE_PATTERN = "\\+?\\d{10,15}";
    private static final String EMAIL_PATTERN = "[^@\\s]+@[^@\\s]+\\.[^@\\s]+";

    private final CustomerRepository customerRepository;
    private final OrderRepository orderRepository;

    public CustomerService(CustomerRepository customerRepository, OrderRepository orderRepository) {
        this.customerRepository = customerRepository;
        this.orderRepository = orderRepository;
    }

    public Customer create(String fullName, String phone, String email, String city, LocalDate registeredAt) {
        Customer customer = new Customer(fullName, phone, email, city, registeredAt);
        validate(customer);
        return customerRepository.save(customer);
    }

    public List<Customer> getAll() {
        return customerRepository.findAll();
    }

    public Customer getById(int id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Клиент", id));
    }

    public Customer update(int id, String fullName, String phone, String email, String city, LocalDate registeredAt) {
        Customer customer = getById(id);
        customer.setFullName(fullName);
        customer.setPhone(phone);
        customer.setEmail(email);
        customer.setCity(city);
        customer.setRegisteredAt(registeredAt);
        validate(customer);
        customerRepository.update(customer);
        return customer;
    }

    /** Бизнес-правило: нельзя удалить клиента, у которого есть заказы. */
    public void delete(int id) {
        Customer customer = getById(id);
        int orders = orderRepository.countByCustomerId(id);
        if (orders > 0) {
            throw new BusinessException("Нельзя удалить клиента «" + customer.getFullName()
                    + "»: у него есть заказы (" + orders + " шт.). Сначала удалите его заказы.");
        }
        customerRepository.deleteById(id);
    }

    /**
     * Поиск и фильтрация клиентов: text ищется без учёта регистра в ФИО, телефоне и email,
     * city — точное совпадение города. Пустое условие (null или пустая строка) не ограничивает выборку.
     */
    public boolean matches(Customer customer, String text, String city) {
        if (city != null && !city.equals(customer.getCity())) {
            return false;
        }
        if (text == null || text.isBlank()) {
            return true;
        }
        String query = text.trim().toLowerCase(Locale.ROOT);
        return customer.getFullName().toLowerCase(Locale.ROOT).contains(query)
                || customer.getPhone().contains(query)
                || customer.getEmail().toLowerCase(Locale.ROOT).contains(query);
    }

    /** Список городов без повторов — для фильтра «Город» на вкладке клиентов. */
    public List<String> getCities(List<Customer> customers) {
        return customers.stream()
                .map(Customer::getCity)
                .distinct()
                .sorted()
                .toList();
    }

    /**
     * Проверяет все поля клиента. Строки очищаются от пробелов по краям и записываются обратно.
     * Уникальность телефона и email проверяется заранее, чтобы показать понятное сообщение у нужного поля
     * (ограничение UNIQUE в базе остаётся второй линией защиты).
     */
    private void validate(Customer customer) {
        customer.setFullName(Validation.requireText(customer.getFullName(), FIELD_FULL_NAME, "ФИО", 100));

        String phone = Validation.requireText(customer.getPhone(), FIELD_PHONE, "Телефон", 20).replaceAll("[\\s()-]", "");
        if (!phone.matches(PHONE_PATTERN)) {
            throw new ValidationException(FIELD_PHONE, "Телефон должен содержать от 10 до 15 цифр, например +79161234567.");
        }
        customer.setPhone(phone);

        String email = Validation.requireText(customer.getEmail(), FIELD_EMAIL, "Email", 100);
        if (!email.matches(EMAIL_PATTERN)) {
            throw new ValidationException(FIELD_EMAIL, "Некорректный email, пример: ivanov@mail.ru.");
        }
        customer.setEmail(email);

        customer.setCity(Validation.requireText(customer.getCity(), FIELD_CITY, "Город", 50));

        LocalDate date = customer.getRegisteredAt();
        if (date == null) {
            throw new ValidationException(FIELD_REGISTERED_AT, "Укажите дату регистрации клиента.");
        }
        if (date.isAfter(LocalDate.now())) {
            throw new ValidationException(FIELD_REGISTERED_AT, "Дата регистрации не может быть в будущем.");
        }
        if (date.isBefore(MIN_REGISTRATION_DATE)) {
            throw new ValidationException(FIELD_REGISTERED_AT, "Дата регистрации не может быть раньше "
                    + MIN_REGISTRATION_DATE.getYear() + " года.");
        }

        if (customerRepository.existsByPhone(phone, customer.getId())) {
            throw new ValidationException(FIELD_PHONE, "Клиент с телефоном " + phone + " уже есть в базе.");
        }
        if (customerRepository.existsByEmail(email, customer.getId())) {
            throw new ValidationException(FIELD_EMAIL, "Клиент с email " + email + " уже есть в базе.");
        }
    }
}
