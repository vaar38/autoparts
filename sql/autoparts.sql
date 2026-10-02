-- =====================================================================
--  База данных «Магазин автозапчастей — заказы»
--  Запуск (из папки проекта):
--    psql -U postgres -c "CREATE DATABASE autoparts"
--    psql -U postgres -d autoparts -f sql/autoparts.sql
--  Скрипт можно запускать повторно: он пересоздаёт таблицы и тестовые данные.
-- =====================================================================

SET client_encoding = 'UTF8';

DROP TABLE IF EXISTS orders;
DROP TABLE IF EXISTS customers;

-- ---------------------------------------------------------------------
--  Клиенты магазина (участник предметной области)
-- ---------------------------------------------------------------------
CREATE TABLE customers (
    id            SERIAL       PRIMARY KEY,
    full_name     VARCHAR(100) NOT NULL,
    phone         VARCHAR(20)  NOT NULL UNIQUE,
    email         VARCHAR(100) NOT NULL UNIQUE,
    city          VARCHAR(50)  NOT NULL,
    registered_at DATE         NOT NULL DEFAULT CURRENT_DATE
);

-- ---------------------------------------------------------------------
--  Заказы автозапчастей (основная сущность варианта)
-- ---------------------------------------------------------------------
CREATE TABLE orders (
    id          SERIAL        PRIMARY KEY,
    customer_id INTEGER       NOT NULL,
    part_name   VARCHAR(100)  NOT NULL,
    part_number VARCHAR(30)   NOT NULL,
    car_brand   VARCHAR(50)   NOT NULL,
    category    VARCHAR(20)   NOT NULL,
    quantity    INTEGER       NOT NULL,
    unit_price  NUMERIC(10,2) NOT NULL,
    status      VARCHAR(20)   NOT NULL DEFAULT 'NEW',
    created_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Связь «многие к одному»: у одного клиента может быть много заказов.
    -- ON DELETE RESTRICT запрещает удалить клиента, у которого есть заказы.
    CONSTRAINT fk_orders_customer FOREIGN KEY (customer_id)
        REFERENCES customers (id) ON DELETE RESTRICT,

    -- Ограничения допустимых значений
    CONSTRAINT chk_orders_quantity CHECK (quantity BETWEEN 1 AND 100),
    CONSTRAINT chk_orders_price    CHECK (unit_price > 0),
    CONSTRAINT chk_orders_category CHECK (category IN
        ('ENGINE', 'BRAKES', 'SUSPENSION', 'ELECTRICAL', 'FILTERS', 'BODY')),
    CONSTRAINT chk_orders_status   CHECK (status IN
        ('NEW', 'CONFIRMED', 'SHIPPED', 'DELIVERED', 'CANCELLED'))
);

-- ---------------------------------------------------------------------
--  Тестовые данные
-- ---------------------------------------------------------------------
INSERT INTO customers (full_name, phone, email, city, registered_at) VALUES
    ('Иванов Иван Петрович',       '+79161234567', 'ivanov@mail.ru',    'Москва',   '2026-01-15'),
    ('Петрова Анна Сергеевна',     '+79262345678', 'petrova@yandex.ru', 'Москва',   '2026-02-03'),
    ('Сидоров Алексей Викторович', '+79033456789', 'sidorov@gmail.com', 'Химки',    '2026-03-10'),
    ('Кузнецова Мария Олеговна',   '+79154567890', 'kuznetsova@mail.ru','Подольск', '2026-04-22'),
    ('Смирнов Дмитрий Андреевич',  '+79855678901', 'smirnov@yandex.ru', 'Москва',   '2026-05-07'),
    ('Волков Сергей Николаевич',   '+79996789012', 'volkov@bk.ru',      'Люберцы',  '2026-06-18');

INSERT INTO orders (customer_id, part_name, part_number, car_brand, category, quantity, unit_price, status, created_at) VALUES
    (1, 'Тормозные колодки передние', '0986494524',  'Toyota',     'BRAKES',     1,  3450.00, 'DELIVERED', '2026-07-02 10:15'),
    (1, 'Масляный фильтр',            'W7008',       'Toyota',     'FILTERS',    2,   690.00, 'DELIVERED', '2026-07-05 12:40'),
    (2, 'Амортизатор передний',       '334842',      'Kia',        'SUSPENSION', 2,  5200.00, 'SHIPPED',   '2026-08-11 09:30'),
    (2, 'Воздушный фильтр',           'C27009',      'Kia',        'FILTERS',    1,   820.00, 'CANCELLED', '2026-08-12 14:05'),
    (3, 'Свечи зажигания',            'BKR6E',       'Hyundai',    'ENGINE',     4,   450.00, 'DELIVERED', '2026-07-20 16:20'),
    (3, 'Ремень ГРМ',                 'CT1028',      'Volkswagen', 'ENGINE',     1,  4100.00, 'CONFIRMED', '2026-09-01 11:00'),
    (4, 'Аккумулятор 60 Ач',          '5604080543',  'Renault',    'ELECTRICAL', 1,  8900.00, 'NEW',       '2026-09-20 10:45'),
    (4, 'Фара передняя левая',        '260604BA0A',  'Nissan',     'BODY',       1, 12500.00, 'SHIPPED',   '2026-09-10 13:10'),
    (5, 'Тормозной диск передний',    '0986479R32',  'BMW',        'BRAKES',     2,  6700.00, 'CONFIRMED', '2026-09-15 15:30'),
    (5, 'Стойка стабилизатора',       '31356765933', 'BMW',        'SUSPENSION', 2,  1850.00, 'NEW',       '2026-09-22 09:05'),
    (6, 'Генератор',                  '0124525093',  'Ford',       'ELECTRICAL', 1, 21400.00, 'NEW',       '2026-09-24 17:50'),
    (6, 'Салонный фильтр',            'CUK2939',     'Ford',       'FILTERS',    1,   950.00, 'CANCELLED', '2026-08-28 08:40'),
    (1, 'Рычаг подвески',             '4806809',     'Toyota',     'SUSPENSION', 1,  7300.00, 'CONFIRMED', '2026-09-18 12:00'),
    (2, 'Зеркало боковое правое',     '876200',      'Kia',        'BODY',       1,  4600.00, 'DELIVERED', '2026-07-28 18:25'),
    (3, 'Помпа водяная',              'PA10085',     'Volkswagen', 'ENGINE',     1,  3900.00, 'NEW',       '2026-09-26 10:10');
