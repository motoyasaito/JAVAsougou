package com.example.demo.mapper;

import com.example.demo.entity.Redelivery;

import java.time.LocalDateTime;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface RedeliveryMapper {
  // 再配達の登録をする。登録後、採番されたidが引数のredeliveryに入る
  @Insert("""
        INSERT INTO redeliveries (delivery_id, redelivery_date, redelivery_time)
        VALUES (#{deliveryId}, #{redeliveryDate}, #{redeliveryTime})
      """)
  @Options(useGeneratedKeys = true, keyProperty = "id")
  int insert(Redelivery redelivery);

  // 完了から基準日時より前に更新された配送物の、再配達を削除する（B001 月次削除バッチで使う
  // 配送物（deliveries）より先に消す。redeliveries.delivery_id が deliveries
  // を参照しているので、親を先に消すと外部キーのエラーになるため
  // DELETE FROM redeliveries ：redeliveries テーブルから行を削除する
  // WHERE delivery_id IN ( ) ：delivery_id が、カッコの中で作った id の一覧のどれかに当てはまる行だけを対象にする
  // SELECT id FROM deliveries ：deliveries テーブルから id の一覧を作る
  // WHERE status = 'DELIVERED' AND updated_at <
  // #{threshold}：配達完了で、かつ基準日時より前に更新されたものだけに絞る（< なので、ちょうど基準日時のものは消さない）
  @Delete("""
            DELETE FROM redeliveries
            WHERE delivery_id IN (
            SELECT id FROM deliveries
            WHERE status = 'DELIVERED' AND updated_at < #{threshold})
      """)
  int deleteByCompletedDeliveries(@Param("threshold") LocalDateTime threshold);// threshold（基準日時）を受け取り、#{threshold}に入れる。戻り値は削除した件数
}
