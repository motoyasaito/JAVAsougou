package com.example.demo.mapper;

import com.example.demo.entity.DriverOption;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper {
  // 指定したidのユーザーがドライバーとして存在するか確認する
  // EXISTS カッコの中のSELECTで1行でも見つかればtrue、 なければfalseを返す
  @Select("""
      SELECT EXISTS (
      SELECT 1 FROM users WHERE id = #{id} AND role = 'driver')
        """)
  boolean existsDriverById(@Param("id") Long id);

  // ドライバー全員を、担当エリアの名前と一緒に取得する（割り当てのプルダウンに使う）
  // LEFT JOIN：エリアが決まっていない（area_id が NULL の）ドライバーも消さずに残す。そのときの area_name は NULL
  @Select("""
      SELECT u.id, u.name, a.name AS area_name
      FROM users u
      LEFT JOIN areas a ON u.area_id = a.id
      WHERE u.role = 'driver'
      ORDER BY u.area_id, u.id
      """)
  List<DriverOption> findDrivers();
}
