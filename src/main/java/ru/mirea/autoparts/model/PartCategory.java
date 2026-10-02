package ru.mirea.autoparts.model;

/** Категория автозапчасти. */
public enum PartCategory {
    ENGINE("Двигатель"),
    BRAKES("Тормоза"),
    SUSPENSION("Подвеска"),
    ELECTRICAL("Электрика"),
    FILTERS("Фильтры"),
    BODY("Кузов");

    private final String title;

    PartCategory(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }
}
