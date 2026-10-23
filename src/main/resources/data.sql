-- エリア
INSERT INTO areas (name) VALUES
    ('埼玉'),
    ('東京'),
    ('神奈川'),
    ('千葉');

-- 従業員（password_hash はダミー値。ログイン実装時に BCrypt 等で生成した値に差し替える）
INSERT INTO users (name, role, call_number, email, area_id, password_hash) VALUES
    ('管理 太郎',   'admin',  '09011110000', 'admin@example.com',   NULL, 'dummy_hash_admin'),
    ('配送 一郎',   'driver', '09011110001', 'driver1@example.com', 1,    'dummy_hash_driver1'),
    ('配送 二郎',   'driver', '09011110002', 'driver2@example.com', 2,    'dummy_hash_driver2'),
    ('配送 三郎',   'driver', '09011110003', 'driver3@example.com', 3,    'dummy_hash_driver3'),
    ('配送 四郎',   'driver', '09011110004', 'driver4@example.com', 4,    'dummy_hash_driver4');

-- 配送物
INSERT INTO deliveries (tracking_number, name, address, call_number, email, status, item_type, driver_id, delivery_date, delivery_time) VALUES
    ('100000000001', '山田 花子', '埼玉県さいたま市浦和区1-1-1', '08022220001', 'yamada@example.com',    'PREPARING',        'NORMAL',   2, '2026-10-23', '午前中'),
    ('100000000002', '佐藤 健',   '東京都新宿区西新宿2-2-2',     '08022220002', 'sato@example.com',      'IN TRANSIT',       'RAW FOOD', 3, '2026-10-23', '14:00-16:00'),
    ('100000000003', '鈴木 美咲', '神奈川県横浜市中区3-3-3',     '08022220003', 'suzuki@example.com',    'IN TRANSIT',       'FROZEN',   4, '2026-10-22', '18:00-20:00'),
    ('100000000004', '高橋 翔',   '千葉県千葉市中央区4-4-4',     '08022220004', 'takahashi@example.com', 'DELIVERED',        'FRAGILE',  5, '2026-10-21', '午前中'),
    ('100000000005', '田中 優',   '東京都渋谷区道玄坂5-5-5',     '08022220005', 'tanaka@example.com',    'ABSENT',           'NORMAL',   3, '2026-10-21', '12:00-14:00');

-- 再配達（不在だった配送物 id=5 の再配達依頼）
INSERT INTO redeliveries (delivery_id, redelivery_date, redelivery_time) VALUES
    (5, '2026-10-24', '18:00-20:00');
