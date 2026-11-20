package com.example.demo.entity;

// 割り当てのプルダウンに並べるドライバー1人分（users と areas を結合した結果を入れる）
public class DriverOption {
  private Long id;
  private String name;
  private String areaName;// areas.name。SQL で AS area_name と名前を付けて、ここに入れる

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getAreaName() {
    return areaName;
  }

  public void setAreaName(String areaName) {
    this.areaName = areaName;
  }
}
