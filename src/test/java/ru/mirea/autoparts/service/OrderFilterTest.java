package ru.mirea.autoparts.service;

import org.junit.jupiter.api.Test;
import ru.mirea.autoparts.exception.ValidationException;
import ru.mirea.autoparts.model.Order;
import ru.mirea.autoparts.model.OrderStatus;
import ru.mirea.autoparts.model.PartCategory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Проверки поиска и фильтров заказов — без базы данных, на объектах в памяти. */
class OrderFilterTest {

    private final Order brakes = new Order(1, 1, "Иванов Иван Петрович", "Тормозные колодки передние", "0986494524",
            "Toyota", PartCategory.BRAKES, 1, new BigDecimal("3450.00"), OrderStatus.DELIVERED,
            LocalDateTime.of(2026, 7, 2, 10, 15));

    @Test
    void emptyFilterMatchesEverything() {
        assertThat(new OrderFilter(null, null, null, null, null).matches(brakes)).isTrue();
        assertThat(new OrderFilter("   ", null, null, null, null).matches(brakes)).isTrue();
    }

    @Test
    void searchLooksInSeveralFieldsIgnoringCase() {
        assertThat(new OrderFilter("КОЛОДКИ", null, null, null, null).matches(brakes)).isTrue();   // название
        assertThat(new OrderFilter("0986", null, null, null, null).matches(brakes)).isTrue();      // артикул
        assertThat(new OrderFilter("toyota", null, null, null, null).matches(brakes)).isTrue();    // марка
        assertThat(new OrderFilter("иванов", null, null, null, null).matches(brakes)).isTrue();    // клиент
        assertThat(new OrderFilter("фильтр", null, null, null, null).matches(brakes)).isFalse();
    }

    @Test
    void statusAndCategoryFiltersWorkTogether() {
        assertThat(new OrderFilter(null, OrderStatus.DELIVERED, PartCategory.BRAKES, null, null).matches(brakes)).isTrue();
        assertThat(new OrderFilter(null, OrderStatus.NEW, PartCategory.BRAKES, null, null).matches(brakes)).isFalse();
        assertThat(new OrderFilter(null, OrderStatus.DELIVERED, PartCategory.ENGINE, null, null).matches(brakes)).isFalse();
    }

    @Test
    void periodBoundsAreInclusive() {
        LocalDate day = LocalDate.of(2026, 7, 2);
        assertThat(new OrderFilter(null, null, null, day, day).matches(brakes)).isTrue();
        assertThat(new OrderFilter(null, null, null, day.plusDays(1), null).matches(brakes)).isFalse();
        assertThat(new OrderFilter(null, null, null, null, day.minusDays(1)).matches(brakes)).isFalse();
    }

    @Test
    void periodStartAfterEndIsRejected() {
        assertThatThrownBy(() -> new OrderFilter(null, null, null, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 8, 1)))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("не может быть позже");
    }
}
