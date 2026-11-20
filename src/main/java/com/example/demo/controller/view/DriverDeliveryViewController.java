package com.example.demo.controller.view;

import com.example.demo.dto.DeliveryResponse;
import com.example.demo.dto.DriverOptionResponse;
import com.example.demo.dto.PageResponse;
import com.example.demo.service.DeliveryService;
import com.example.demo.service.DriverService;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

// ドライバーの「本日の配達物」の画面を受け持つController
// TODO: ログイン機能ができたら、driverId は URL で受け取らず、ログイン中のドライバーの ID を使う（上の帯の切り替えもなくす）
@Controller
@RequestMapping("/driver/deliveries")
public class DriverDeliveryViewController {

  // 1日に1人が配る件数はそれほど多くないので、ページに分けずに1画面で全部出す（A003 の size の上限と同じ）
  private static final int MAX_SIZE = 100;

  private final DeliveryService deliveryService;
  private final DriverService driverService;
  private final Clock clock;// 「今」を知る時計（TimeConfig で日本時間にしている）

  public DriverDeliveryViewController(DeliveryService deliveryService, DriverService driverService, Clock clock) {
    this.deliveryService = deliveryService;
    this.driverService = driverService;
    this.clock = clock;
  }

  // 配達物の一覧を表示する。例: /driver/deliveries?driverId=2&date=2026-11-21（date を省略すると今日）
  @GetMapping
  public String list(@RequestParam(required = false) Long driverId,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
      Model model) {
    List<DriverOptionResponse> drivers = driverService.findDrivers();
    model.addAttribute("drivers", drivers);// 上の帯の切り替え（ログインができるまでの仮）に使う

    LocalDate today = LocalDate.now(clock);// 日本時間の今日
    LocalDate targetDate = (date != null) ? date : today;// 条件 ? A : B は「条件が正しければ A、違えば B」
    model.addAttribute("date", targetDate);
    model.addAttribute("today", today);

    // ドライバーの中から、指定された人を探す。見つからない（未指定・ドライバーでない）ときは、一覧を出さずに選んでもらう
    DriverOptionResponse driver = drivers.stream()
        .filter(d -> d.id().equals(driverId))// filter：条件に合うものだけを残す
        .findFirst()// 最初の1人を取る。いなければ「空」
        .orElse(null);// 空なら null にする
    if (driver == null) {
      return "driver/deliveries";// JSP 側で「ドライバーを選んでください」と出す
    }

    model.addAttribute("driver", driver);
    PageResponse<DeliveryResponse> result = deliveryService.findByDriver(driver.id(), targetDate, 0, MAX_SIZE);// A003 と同じServiceのメソッド
    model.addAttribute("deliveries", result.content());
    model.addAttribute("total", result.totalElements());
    return "driver/deliveries";// /WEB-INF/jsp/driver/deliveries.jsp を表示する
  }
}
