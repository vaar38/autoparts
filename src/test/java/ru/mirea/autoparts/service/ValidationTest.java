package ru.mirea.autoparts.service;

import org.junit.jupiter.api.Test;
import ru.mirea.autoparts.exception.ValidationException;
import ru.mirea.autoparts.model.OrderStatus;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Проверки разбора и валидации пользовательского ввода, а также правил смены статуса. */
class ValidationTest {

    @Test
    void requireTextTrimsAndChecksLength() {
        assertThat(Validation.requireText("  Toyota ", "carBrand", "Марка", 50)).isEqualTo("Toyota");
        assertThatThrownBy(() -> Validation.requireText("  ", "carBrand", "Марка", 50))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("обязательно");
        assertThatThrownBy(() -> Validation.requireText("x".repeat(51), "carBrand", "Марка", 50))
                .hasMessageContaining("не более 50");
    }

    @Test
    void parseIntChecksNumberAndRange() {
        assertThat(Validation.parseInt(" 5 ", "quantity", "Количество", 1, 100)).isEqualTo(5);
        assertThatThrownBy(() -> Validation.parseInt("пять", "quantity", "Количество", 1, 100))
                .hasMessageContaining("целым числом");
        assertThatThrownBy(() -> Validation.parseInt("101", "quantity", "Количество", 1, 100))
                .hasMessageContaining("от 1 до 100");
    }

    @Test
    void parseMoneyAcceptsCommaAndRejectsExtraDigits() {
        assertThat(Validation.parseMoney("3 450,50", "unitPrice", "Цена")).isEqualByComparingTo(new BigDecimal("3450.50"));
        assertThatThrownBy(() -> Validation.parseMoney("10.999", "unitPrice", "Цена"))
                .hasMessageContaining("2 знаков");
        assertThatThrownBy(() -> Validation.parseMoney("abc", "unitPrice", "Цена"))
                .isInstanceOfSatisfying(ValidationException.class, e -> assertThat(e.getField()).isEqualTo("unitPrice"));
    }

    @Test
    void statusTransitionsFollowBusinessRules() {
        assertThat(OrderStatus.NEW.canChangeTo(OrderStatus.CONFIRMED)).isTrue();
        assertThat(OrderStatus.NEW.canChangeTo(OrderStatus.DELIVERED)).isFalse();
        assertThat(OrderStatus.SHIPPED.getAllowedNext()).containsExactly(OrderStatus.DELIVERED);
        assertThat(OrderStatus.CANCELLED.getAllowedNext()).isEmpty();
    }
}
