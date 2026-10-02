package ru.mirea.autoparts.service;

import ru.mirea.autoparts.exception.BusinessException;
import ru.mirea.autoparts.exception.EntityNotFoundException;
import ru.mirea.autoparts.model.Customer;
import ru.mirea.autoparts.repository.CustomerRepository;
import ru.mirea.autoparts.repository.OrderRepository;

import java.util.List;

/** Бизнес-логика для клиентов: проверки данных и правила перед обращением к базе. */
public class CustomerService {
    private static final String PHONE_PATTERN = "\\+?\\d{10,15}";
    private static final String EMAIL_PATTERN = "[^@\\s]+@[^@\\s]+\\.[^@\\s]+";

    private final CustomerRepository customerRepository;
    private final OrderRepository orderRepository;

    public CustomerService(CustomerRepository customerRepository, OrderRepository orderRepository) {
        this.customerRepository = customerRepository;
        this.orderRepository = orderRepository;
    }

    public Customer create(String fullName, String phone, String email, String city) {
        validate(fullName, phone, email, city);
        return customerRepository.save(new Customer(fullName, phone, email, city));
    }

    public List<Customer> getAll() {
        return customerRepository.findAll();
    }

    public Customer getById(int id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Клиент", id));
    }

    public Customer update(int id, String fullName, String phone, String email, String city) {
        Customer customer = getById(id);
        validate(fullName, phone, email, city);
        customer.setFullName(fullName);
        customer.setPhone(phone);
        customer.setEmail(email);
        customer.setCity(city);
        customerRepository.update(customer);
        return customer;
    }

    /** Бизнес-правило: нельзя удалить клиента, у которого есть заказы. */
    public void delete(int id) {
        getById(id);
        int orders = orderRepository.countByCustomerId(id);
        if (orders > 0) {
            throw new BusinessException("Нельзя удалить клиента с ID " + id
                    + ": у него есть заказы (" + orders + " шт.). Сначала удалите его заказы.");
        }
        customerRepository.deleteById(id);
    }

    private void validate(String fullName, String phone, String email, String city) {
        if (fullName == null || fullName.isBlank()) {
            throw new BusinessException("ФИО клиента не может быть пустым.");
        }
        if (phone == null || !phone.matches(PHONE_PATTERN)) {
            throw new BusinessException("Телефон должен содержать от 10 до 15 цифр, например +79161234567.");
        }
        if (email == null || !email.matches(EMAIL_PATTERN)) {
            throw new BusinessException("Некорректный email, пример: ivanov@mail.ru.");
        }
        if (city == null || city.isBlank()) {
            throw new BusinessException("Город не может быть пустым.");
        }
    }
}
