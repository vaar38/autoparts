package ru.mirea.autoparts.fx.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import ru.mirea.autoparts.fx.AppContext;
import ru.mirea.autoparts.fx.util.Dialogs;

/**
 * Контроллер главного окна (MainView.fxml): меню, заголовок и вкладки.
 * Содержимое вкладок подключено в FXML через fx:include, у каждой вкладки свой контроллер.
 * FXMLLoader сам кладёт вложенные контроллеры в поля с именем «fx:id + Controller».
 */
public class MainController {
    private final AppContext context;

    @FXML private TabPane tabPane;
    @FXML private Tab ordersTab;
    @FXML private Tab customersTab;
    @FXML private Tab statisticsTab;
    @FXML private Label connectionLabel;

    // Вложенные контроллеры из <fx:include fx:id="ordersView" .../> и т.д.
    @FXML private OrdersController ordersViewController;
    @FXML private CustomersController customersViewController;
    @FXML private StatisticsController statisticsViewController;

    public MainController(AppContext context) {
        this.context = context;
    }

    @FXML
    private void initialize() {
        connectionLabel.setText("База данных: " + context.getDatabaseUrl());
        // При переходе на вкладку перечитываем её данные — так изменения на одной вкладке видны на другой
        tabPane.getSelectionModel().selectedItemProperty().addListener((obs, old, tab) -> refreshTab(tab));
    }

    private void refreshTab(Tab tab) {
        if (tab == ordersTab) {
            ordersViewController.refresh();
        } else if (tab == customersTab) {
            customersViewController.refresh();
        } else if (tab == statisticsTab) {
            statisticsViewController.refresh();
        }
    }

    @FXML
    private void onRefresh() {
        refreshTab(tabPane.getSelectionModel().getSelectedItem());
    }

    @FXML
    private void onExport() {
        tabPane.getSelectionModel().select(ordersTab);
        ordersViewController.onExport();
    }

    @FXML
    private void onShowOrders() {
        tabPane.getSelectionModel().select(ordersTab);
    }

    @FXML
    private void onShowCustomers() {
        tabPane.getSelectionModel().select(customersTab);
    }

    @FXML
    private void onShowStatistics() {
        tabPane.getSelectionModel().select(statisticsTab);
    }

    @FXML
    private void onAbout() {
        Dialogs.info("Магазин автозапчастей",
                "Учёт заказов автозапчастей.\nКонтрольная работа №2: JavaFX + PostgreSQL (JDBC).\n\n"
                        + "Двойной щелчок по строке — изменить, Delete — удалить,\n"
                        + "щелчок по заголовку столбца — сортировка (Shift+щелчок — по нескольким столбцам).");
    }

    @FXML
    private void onExit() {
        Platform.exit();
    }
}
