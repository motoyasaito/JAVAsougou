package com.example.demo.mapper;

import com.example.demo.entity.Redelivery;

import java.util.Optional;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface RedeliveryMapper {
  // 再配達の登録をする。登録後、採番されたidが引数のredeliveryに入る
  @Insert("""
        INSERT INTO redeliveries (delivery_id, redelivery_date, redelivery_time)
        VALUES (#{deliveryId}, #{redeliveryDate}, #{redeliveryTime})
      """)
  @Options(useGeneratedKeys = true, keyProperty = "id")
  int insert(Redelivery redelivery);

  // 再配達を1件取得する
  @Select("""
        SELECT id, delivery_id, allocation_status, redelivery_date, redelivery_time, created_at, updated_at
        FROM redeliveries
        WHERE id = #{id}
      """)
  Optional<Redelivery> findById(@Param("id") long id);

  // allocationStatusをDONEにする
  @Update("""
        UPDATE redeliveries
        SET allocation_status = #{allocationStatus}, updated_at = now()
        WHERE id = #{id}
      """)
  int updateAllocationStatusById(@Param("id") long id, @Param("allocationStatus") String allocationStatus);
}
