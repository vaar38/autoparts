package ru.mirea.autoparts.fx.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import ru.mirea.autoparts.exception.ValidationException;
import ru.mirea.autoparts.fx.AppContext;
import ru.mirea.autoparts.fx.util.FormErrors;
import ru.mirea.autoparts.fx.util.UiActions;
import ru.mirea.autoparts.model.OrderStatus;
import ru.mirea.autoparts.model.PartCategory;
import ru.mirea.autoparts.service.StatisticsService;
import ru.mirea.autoparts.util.Formats;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * Контроллер вкладки «Статистика» (StatisticsView.fxml).
 * Сами показатели считает StatisticsService; контроллер только раскладывает их по карточкам и диаграммам.
 */
public class StatisticsController {
    private final StatisticsService statisticsService;

    @FXML private FlowPane cardsPane;
    @FXML private DatePicker periodFromPicker;
    @FXML private DatePicker periodToPicker;
    @FXML private FlowPane periodCardsPane;
    @FXML private Label periodErrorLabel;
    @FXML private PieChart categoryChart;
    @FXML private BarChart<String, Number> statusChart;
    @FXML private NumberAxis statusCountAxis;

    public StatisticsController(AppContext context) {
        this.statisticsService = context.getStatisticsService();
    }

    @FXML
    private void initialize() {
        periodFromPicker.setValue(LocalDate.now().withDayOfMonth(1).minusMonths(2));
        periodToPicker.setValue(LocalDate.now());
        periodErrorLabel.setManaged(false);
        // Количество заказов — целое число, поэтому дробные деления на оси не подписываем
        statusCountAxis.setTickLabelFormatter(new StringConverter<>() {
            @Override
            public String toString(Number value) {
                return value.doubleValue() % 1 == 0 ? String.valueOf(value.intValue()) : "";
            }

            @Override
            public Number fromString(String text) {
                return Integer.valueOf(text);
            }
        });
    }

    /** Пересчитывает всё. Вызывается при открытии вкладки и кнопкой «Обновить». */
    @FXML
    public void refresh() {
        UiActions.run("Расчёт статистики", () -> {
            fillCards(cardsPane, statisticsService.collect());
            fillCategoryChart(statisticsService.totalByCategory());
            fillStatusChart(statisticsService.countByStatus());
            calculatePeriod();
        });
    }

    @FXML
    private void onCalculatePeriod() {
        UiActions.run("Расчёт статистики за период", this::calculatePeriod);
    }

    private void calculatePeriod() {
        periodFromPicker.getStyleClass().remove(FormErrors.ERROR_CLASS);
        periodToPicker.getStyleClass().remove(FormErrors.ERROR_CLASS);
        periodErrorLabel.setVisible(false);
        periodErrorLabel.setManaged(false);
        try {
            fillCards(periodCardsPane, statisticsService.collectForPeriod(periodFromPicker.getValue(), periodToPicker.getValue()));
        } catch (ValidationException e) {
            periodCardsPane.getChildren().clear();
            periodErrorLabel.setText(e.getMessage());
            periodErrorLabel.setVisible(true);
            periodErrorLabel.setManaged(true);
            periodFromPicker.getStyleClass().add(FormErrors.ERROR_CLASS);
            periodToPicker.getStyleClass().add(FormErrors.ERROR_CLASS);
        }
    }

    /** Каждый показатель — «карточка»: крупное значение и подпись под ним. Внешний вид задан в CSS (.stat-card). */
    private void fillCards(FlowPane pane, Map<String, String> stats) {
        pane.getChildren().clear();
        stats.forEach((name, value) -> {
            Label valueLabel = new Label(value);
            valueLabel.getStyleClass().add("stat-value");
            valueLabel.setWrapText(true);
            Label nameLabel = new Label(name);
            nameLabel.getStyleClass().add("stat-name");
            nameLabel.setWrapText(true);
            VBox card = new VBox(4, valueLabel, nameLabel);
            card.getStyleClass().add("stat-card");
            pane.getChildren().add(card);
        });
    }

    private void fillCategoryChart(Map<PartCategory, BigDecimal> totals) {
        ObservableList<PieChart.Data> data = FXCollections.observableArrayList();
        totals.forEach((category, total) ->
                data.add(new PieChart.Data(category.getTitle() + "\n" + Formats.money(total), total.doubleValue())));
        categoryChart.setData(data);
    }

    private void fillStatusChart(Map<OrderStatus, Long> counts) {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Заказов");
        counts.forEach((status, count) -> series.getData().add(new XYChart.Data<>(status.getTitle(), count)));
        statusChart.getData().setAll(series);
    }
}
