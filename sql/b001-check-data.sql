-- B001 完了ステータスの月次削除バッチの動作確認用データ
-- アプリ起動後（data.sqlが流れた後）に手動で流す。src/main/resources に置くと起動のたびに流れるので、ここに置いている
-- 削除の条件：status = 'DELIVERED' かつ updated_at が基準日時（実行日時の1か月前）より前

-- A：完了から2か月・再配達あり → 再配達ごと削除される
-- updated_at を now() - interval '2 months'（今から2か月前）にして、1か月より前に完了した状態を作る
INSERT INTO deliveries(tracking_number, name, address, call_number, email, status, item_type, driver_id, delivery_date, delivery_time, updated_at)
VALUES ('300000000001', 'テストA', 'テスト住所', '07011112222', 'test@example.com', 'DELIVERED', 'NORMAL', 2, '2026-08-20', '午前中', now() - interval  '2 months');

-- A の再配達。delivery_id は自動採番で何番になるか分からないので、伝票番号から id を探して入れる
INSERT INTO redeliveries(delivery_id, redelivery_date, redelivery_time)
VALUES((SELECT id FROM deliveries WHERE tracking_number = '300000000001'), '2026-08-20', '午前中');

-- B：完了から10日 → 残る（まだ1か月たっていないため）
INSERT INTO deliveries(tracking_number, name, address, call_number, email, status, item_type, driver_id, delivery_date, delivery_time, updated_at)
VALUES ('300000000002', 'テストB', 'テスト住所', '07011112223', 'test@example.com', 'DELIVERED', 'NORMAL', 2, '2026-08-20', '午前中', now() - interval  '10 days');

-- C：配達中で2か月前 → 残る（完了していないため）
INSERT INTO deliveries(tracking_number, name, address, call_number, email, status, item_type, driver_id, delivery_date, delivery_time, updated_at)
VALUES ('300000000003', 'テストC', 'テスト住所', '07011112223', 'test@example.com', 'IN TRANSIT', 'NORMAL', 2, '2026-08-20', '午前中', now() - interval  '2 months');

-- D：完了から2か月・再配達なし → 削除される
INSERT INTO deliveries(tracking_number, name, address, call_number, email, status, item_type, driver_id, delivery_date, delivery_time, updated_at)
VALUES ('300000000004', 'テストD', 'テスト住所', '07011112223', 'test@example.com', 'DELIVERED', 'NORMAL', 2, '2026-08-20', '午前中', now() - interval  '2 months');
