# ER-диаграмма базы данных `autoparts`

Картинка: [er-diagram.svg](er-diagram.svg) (открывается в любом браузере, можно вставить в отчёт).

Та же диаграмма в формате Mermaid (её можно вставить на https://mermaid.live и скачать PNG):

```mermaid
erDiagram
    customers ||--o{ orders : "делает"

    customers {
        SERIAL id PK
        VARCHAR full_name "NOT NULL"
        VARCHAR phone "NOT NULL UNIQUE"
        VARCHAR email "NOT NULL UNIQUE"
        VARCHAR city "NOT NULL"
        DATE registered_at "NOT NULL"
    }

    orders {
        SERIAL id PK
        INTEGER customer_id FK "NOT NULL -> customers.id"
        VARCHAR part_name "NOT NULL"
        VARCHAR part_number "NOT NULL"
        VARCHAR car_brand "NOT NULL"
        VARCHAR category "CHECK: ENGINE, BRAKES, ..."
        INTEGER quantity "CHECK 1..100"
        NUMERIC unit_price "CHECK > 0"
        VARCHAR status "CHECK: NEW, CONFIRMED, ..."
        TIMESTAMP created_at "NOT NULL"
    }
```

## Связь

`orders.customer_id → customers.id` — **один ко многим**: у одного клиента может быть сколько угодно заказов
(в том числе ни одного), а каждый заказ принадлежит ровно одному клиенту.

`ON DELETE RESTRICT` — база не даст удалить клиента, пока у него есть заказы.

## Ограничения

| Ограничение | Где | Зачем |
|-------------|-----|-------|
| PRIMARY KEY | `customers.id`, `orders.id` | уникальный номер каждой строки |
| FOREIGN KEY | `orders.customer_id` | заказ нельзя создать для несуществующего клиента |
| NOT NULL | все столбцы | обязательные поля не могут быть пустыми |
| UNIQUE | `customers.phone`, `customers.email` | два клиента не могут иметь один телефон или email |
| CHECK | `quantity`, `unit_price`, `category`, `status` | допустимые значения: количество 1–100, цена > 0, категория и статус только из списка |
