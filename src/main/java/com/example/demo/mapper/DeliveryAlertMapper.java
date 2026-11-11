package com.example.demo.mapper;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface DeliveryAlertMapper {

  @Delete("""
            DELETE FROM deliveries_alerts
            WHERE delivery_id IN (
            SELECT id FROM deliveries
            WHERE status = 'DELIVERED' AND updated_at < #{threshold})
      """)
  int deleteByCompletedDeliveries(@Param("threshold") LocalDateTime threshold);

  @Insert("""
        INSERT INTO deliveries_alerts (delivery_id, detected_date)
        SELECT d.id, #{today}
        FROM deliveries d
        WHERE d.status = 'IN TRANSIT'
          AND COALESCE(
                (SELECT MAX(r.redelivery_date) FROM redeliveries r WHERE r.delivery_id = d.id),
                d.delivery_date
                ) < #{today}
        ON CONFLICT (delivery_id) DO NOTHING
      """)
  int insertOverdue(@Param("today") LocalDate today);
}
