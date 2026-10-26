package com.example.demo.mapper;

import com.example.demo.entity.Delivery;
import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface DeliveryMapper {

  // 配送物をID順に、指定した件数（limit）だけ、指定した位置（offset）から取得する
  @Select("""
      SELECT id, tracking_number, name, address, call_number, email, status, item_type,
             driver_id, delivery_date, delivery_time, created_at, updated_at
      FROM deliveries
      ORDER BY id
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
}
