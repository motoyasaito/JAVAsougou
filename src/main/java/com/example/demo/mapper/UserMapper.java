package com.example.demo.mapper;

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
}
