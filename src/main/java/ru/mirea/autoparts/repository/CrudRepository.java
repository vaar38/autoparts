package ru.mirea.autoparts.repository;

import java.util.List;
import java.util.Optional;

/**
 * Общий интерфейс для работы с таблицей: CRUD = Create, Read, Update, Delete.
 * Интерфейс описывает, ЧТО умеет репозиторий, а классы CustomerRepository и OrderRepository
 * реализуют, КАК это сделать для своей таблицы.
 *
 * @param <T> тип сущности (Customer, Order)
 */
public interface CrudRepository<T> {

    /** INSERT: сохраняет новую запись и возвращает её с присвоенным id. */
    T save(T entity);

    /** SELECT всех записей. */
    List<T> findAll();

    /** SELECT по id. Optional — «может быть пустым», если запись не найдена. */
    Optional<T> findById(int id);

    /** UPDATE: возвращает true, если запись была изменена. */
    boolean update(T entity);

    /** DELETE: возвращает true, если запись была удалена. */
    boolean deleteById(int id);
}
