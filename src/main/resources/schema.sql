-- 起動のたびに作り直す（参照されている側を後に削除）
DROP TABLE IF EXISTS redeliveries;
DROP TABLE IF EXISTS deliveries;
DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS areas;

-- エリアテーブル
CREATE TABLE areas (
    id   BIGSERIAL PRIMARY KEY,
    name VARCHAR(10) NOT NULL UNIQUE
);

-- 従業員テーブル（ドライバーと管理者を一つのテーブルで管理）
CREATE TABLE users (
    id            BIGSERIAL PRIMARY KEY,
    name          VARCHAR(50)  NOT NULL,
    role          VARCHAR(10)  NOT NULL CHECK (role IN ('admin', 'driver')),
    call_number   VARCHAR(11)  NOT NULL UNIQUE CHECK (call_number ~ '^[0-9]{11}$'),
    email         VARCHAR(100) NOT NULL UNIQUE,
    area_id       BIGINT,  -- 管理者はNULL
    password_hash VARCHAR(255) NOT NULL,
    created_at    TIMESTAMP    NOT NULL DEFAULT now(),
    FOREIGN KEY (area_id) REFERENCES areas(id)
);

-- 配送物テーブル
CREATE TABLE deliveries (
    id              BIGSERIAL PRIMARY KEY,
    tracking_number VARCHAR(12)  NOT NULL UNIQUE CHECK (tracking_number ~ '^[0-9]{12}$'),
    name            VARCHAR(50)  NOT NULL,
    address         VARCHAR(100) NOT NULL,
    call_number     VARCHAR(11)  NOT NULL CHECK (call_number ~ '^[0-9]{11}$'),
    email           VARCHAR(100) NOT NULL,
    status          VARCHAR(50)  CHECK (status IN ('PREPARING', 'IN TRANSIT', 'OUT FOR DELIVERY', 'DELIVERED', 'ABSENT')),
    item_type       VARCHAR(50)  NOT NULL CHECK (item_type IN ('NORMAL', 'RAW FOOD', 'FROZEN', 'FRAGILE')),
    driver_id       BIGINT,
    delivery_date   VARCHAR(20)  NOT NULL,
    delivery_time   VARCHAR(20)  NOT NULL,
    created_at      TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT now(),
    FOREIGN KEY (driver_id) REFERENCES users(id)
);

-- 再配達テーブル
CREATE TABLE redeliveries (
    id              BIGSERIAL PRIMARY KEY,
    delivery_id     BIGINT      NOT NULL,
    driver_id       BIGINT,
    redelivery_date VARCHAR(20) NOT NULL,
    redelivery_time VARCHAR(20) NOT NULL,
    created_at      TIMESTAMP   NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP   NOT NULL DEFAULT now(),
    FOREIGN KEY (delivery_id) REFERENCES deliveries(id),
    FOREIGN KEY (driver_id)   REFERENCES users(id)
);
