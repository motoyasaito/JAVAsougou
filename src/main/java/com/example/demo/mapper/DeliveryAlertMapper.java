package com.example.demo.mapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.example.demo.entity.DeliveryAlert;

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

  @Select("""
      SELECT d.name, d.tracking_number, d.status,
      COALESCE(
              (SELECT MAX(r.redelivery_date) FROM redeliveries r WHERE r.delivery_id = d.id),d.delivery_date) AS delivery_date,
               d.driver_id, a.detected_date, d.updated_at
      FROM deliveries_alerts a
      JOIN deliveries d ON a.delivery_id = d.id
      WHERE d.status != 'DELIVERED'
      ORDER BY a.detected_date, a.id
      LIMIT #{limit} OFFSET #{offset}
      """)
  List<DeliveryAlert> findPending(@Param("limit") int limit, @Param("offset") long offset);

  @Select("""
      SELECT COUNT(*)
      FROM deliveries_alerts a
      JOIN deliveries d ON a.delivery_id = d.id
      WHERE d.status != 'DELIVERED'
      """)
  long countPending();
}
