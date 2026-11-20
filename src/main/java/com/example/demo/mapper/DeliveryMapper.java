package com.example.demo.mapper;

import com.example.demo.entity.Delivery;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface DeliveryMapper {

  // 配送物を登録の新しい順に、指定した件数（limit）だけ、指定した位置（offset）から取得する
  // 登録日時が同じとき（data.sql でまとめて入れたときなど）は、id の大きい順（あとから入れた順）にする
  @Select("""
      SELECT id, tracking_number, name, address, call_number, email, status, item_type,
             driver_id, delivery_date, delivery_time, created_at, updated_at
      FROM deliveries
      ORDER BY created_at DESC, id DESC
      LIMIT #{limit} OFFSET #{offset}
      """)
  List<Delivery> findAll(@Param("limit") int limit, @Param("offset") long offset);

  // 配送物の全件数を取得する（ページ数の計算に使う）
  @Select("SELECT COUNT(*) FROM deliveries")
  long count();

  @Select("""
      SELECT id, tracking_number, name, address, call_number, email, status, item_type,
             driver_id, delivery_date, delivery_time, created_at, updated_at
      FROM deliveries
      WHERE driver_id = #{driverId}
      AND delivery_date = #{deliveryDate}
      ORDER BY delivery_date, id
      LIMIT #{limit} OFFSET #{offset}
      """)
  List<Delivery> findByDriverId(@Param("driverId") long driverId, @Param("deliveryDate") LocalDate deliveryDate,
      @Param("limit") int limit, @Param("offset") long offset);

  @Select("SELECT COUNT(*) FROM deliveries WHERE driver_id = #{driverId} AND delivery_date = #{deliveryDate}")
  long countByDriverId(@Param("driverId") long driverId, @Param("deliveryDate") LocalDate deliveryDate);

  // 配送物を1件取得する
  @Select("""
      SELECT id, tracking_number, name, address, call_number, email, status, item_type,
             driver_id, delivery_date, delivery_time, created_at, updated_at
      FROM deliveries
      WHERE id = #{id}
      """)
  Optional<Delivery> findById(@Param("id") long id);

  // 伝票番号がすでに使われているか確認する
  @Select("SELECT EXISTS (SELECT 1 FROM deliveries WHERE tracking_number = #{trackingNumber})")
  boolean existsByTrackingNumber(@Param("trackingNumber") String trackingNumber);

  // 配送物を登録する。登録後、採番されたidが引数のdeliveryのidに入る
  // status・created_at・updated_atはDBの既定値（PREPARING・now()）に任せる
  @Insert("""
      INSERT INTO deliveries (tracking_number, name, address, call_number, email, item_type,
                              delivery_date, delivery_time)
      VALUES (#{trackingNumber}, #{name}, #{address}, #{callNumber}, #{email}, #{itemType},
              #{deliveryDate}, #{deliveryTime})
      """)
  @Options(useGeneratedKeys = true, keyProperty = "id")
  int insert(Delivery delivery);

  // 伝票番号で配送物を1件取得する
  @Select("""
      SELECT id, tracking_number, name, address, call_number, email, status, item_type,
             driver_id, delivery_date, delivery_time, created_at, updated_at
      FROM deliveries
      WHERE tracking_number = #{trackingNumber}
      """)
  Optional<Delivery> findByTrackingNumber(@Param("trackingNumber") String trackingNumber);

  // 配送状況確認（A008・お客さんの配送状況の画面）用に、伝票番号で配送物を1件取得する
  // 配達予定日と時間帯は、再配達があれば「最新の再配達」の日付と時間帯、なければ最初の配達日と時間帯にする
  // （B002・A014 と同じ「再配達があれば最新の再配達日」という考え方。日付と時間帯は同じ1行から取って、ちぐはぐにしない）
  // LEFT JOIN LATERAL：配送物1件ごとに、カッコの中の SELECT（その配送物の最新の再配達を1行だけ取る）を実行して横につなげる
  //   LEFT なので、再配達がない配送物も消えずに残る（そのときの r の値は NULL）
  // COALESCE(a, b)：a が NULL なら b を使う
  @Select("""
      SELECT d.tracking_number, d.status,
             COALESCE(r.redelivery_date, d.delivery_date) AS delivery_date,
             CASE WHEN r.id IS NULL THEN d.delivery_time ELSE r.redelivery_time END AS delivery_time
      FROM deliveries d
      LEFT JOIN LATERAL (
          SELECT id, redelivery_date, redelivery_time
          FROM redeliveries
          WHERE delivery_id = d.id
          ORDER BY redelivery_date DESC NULLS LAST, id DESC
          LIMIT 1
      ) r ON true
      WHERE d.tracking_number = #{trackingNumber}
      """)
  Optional<Delivery> findStatusByTrackingNumber(@Param("trackingNumber") String trackingNumber);

  // idで配送物のステータスを変更する
  @Update("""
      UPDATE deliveries
      SET status = #{status}, updated_at = now()
      WHERE id = #{id}
      """)
  int updateStatus(@Param("id") long id, @Param("status") String status);

  @Delete("""
        DELETE FROM deliveries
        WHERE status = 'DELIVERED' AND updated_at < #{threshold}
      """)
  int deleteCompleted(@Param("threshold") LocalDateTime threshold);

  // idで配送物の担当ドライバーを変更する
  @Update("""
        UPDATE deliveries
        SET driver_id = #{driverId}, updated_at = now()
        WHERE id = #{id}
      """)
  int updateDriverById(@Param("id") long id, @Param("driverId") Long driverId);
}
