package com.example.demo.mapper;

import com.example.demo.entity.Redelivery;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;

@Mapper
public interface RedeliveryMapper {
  // 再配達の登録をする。登録後、採番されたidが引数のredeliveryに入る
  @Insert("""
        INSERT INTO redeliveries (delivery_id, redelivery_date, redelivery_time)
        VALUES (#{deliveryId}, #{redeliveryDate}, #{redeliveryTime})
      """)
  @Options(useGeneratedKeys = true, keyProperty = "id")
  int insert(Redelivery redelivery);
}
