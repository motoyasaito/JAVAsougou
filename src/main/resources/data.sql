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

-- 今日・明日に配る配送物（ドライバーの「本日の配達物」の画面などで、いつ起動しても見られるようにする）
-- 配達日は CURRENT_DATE（DB が動いている日の今日）。CURRENT_DATE + 1 は明日
-- 上の id=1〜5 の並びを変えないように、最後に足している（再配達が id=5 を使っているため）
INSERT INTO deliveries (tracking_number, name, address, call_number, email, status, item_type, driver_id, delivery_date, delivery_time) VALUES
    -- 配送 一郎（id=2、埼玉）
    ('100000000006', '伊藤 誠',   '埼玉県さいたま市大宮区6-6-6',   '08022220006', 'ito@example.com',       'IN TRANSIT', 'NORMAL',   2, CURRENT_DATE,     '午前中'),
    ('100000000007', '渡辺 彩',   '埼玉県川口市本町7-7-7',         '08022220007', 'watanabe@example.com',  'IN TRANSIT', 'FROZEN',   2, CURRENT_DATE,     '14:00-16:00'),
    ('100000000008', '山本 大輔', '埼玉県所沢市元町8-8-8',         '08022220008', 'yamamoto@example.com',  'PREPARING',  'FRAGILE',  2, CURRENT_DATE,     '18:00-20:00'),
    ('100000000009', '中村 恵',   '埼玉県越谷市越ヶ谷9-9-9',       '08022220009', 'nakamura@example.com',  'PREPARING',  'NORMAL',   2, CURRENT_DATE + 1, '午前中'),
    -- 配送 二郎（id=3、東京）
    ('100000000010', '小林 翼',   '東京都港区芝公園10-10-10',      '08022220010', 'kobayashi@example.com', 'DELIVERED',  'NORMAL',   3, CURRENT_DATE,     '午前中'),
    ('100000000011', '加藤 茜',   '東京都品川区大崎11-11-11',      '08022220011', 'kato@example.com',      'ABSENT',     'RAW FOOD', 3, CURRENT_DATE,     '12:00-14:00'),
    ('100000000012', '吉田 拓海', '東京都世田谷区三軒茶屋12-12-12', '08022220012', 'yoshida@example.com',   'IN TRANSIT', 'NORMAL',   3, CURRENT_DATE,     '16:00-18:00'),
    -- 配送 三郎（id=4、神奈川）
    ('100000000013', '山田 陽菜', '神奈川県川崎市川崎区13-13-13',  '08022220013', 'hina@example.com',      'IN TRANSIT', 'FRAGILE',  4, CURRENT_DATE,     '12:00-14:00'),
    ('100000000014', '佐々木 蓮', '神奈川県相模原市中央区14-14-14', '08022220014', 'sasaki@example.com',    'PREPARING',  'FROZEN',   4, CURRENT_DATE + 1, '16:00-18:00'),
    -- 配送 四郎（id=5、千葉）
    ('100000000015', '松本 結衣', '千葉県船橋市本町15-15-15',      '08022220015', 'matsumoto@example.com', 'IN TRANSIT', 'NORMAL',   5, CURRENT_DATE,     '14:00-16:00'),
    -- まだドライバーを割り当てていない配送物（管理者が割り当てる）
    ('100000000016', '井上 颯',   '東京都中野区中野16-16-16',      '08022220016', 'inoue@example.com',     'PREPARING',  'NORMAL',   NULL, CURRENT_DATE + 1, '午前中');
