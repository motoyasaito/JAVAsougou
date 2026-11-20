package com.example.demo.controller.view;

import com.example.demo.exception.NotFoundException;
import com.example.demo.service.DriverService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

// 管理者の再配達一覧・再配達詳細の「画面」を受け持つController
// TODO: 再配達一覧（A010）・再配達詳細（A013）の処理（Service・Mapper）ができたら、見本のデータをやめて Service から取るように差し替える
// TODO: 再配達の割り振り（A011、reassignDriver ブランチ）が main に入ったら、詳細画面の「割り振る」をつなぐ
// TODO: 認証処理は別チケットで対応（管理者のみ開けるようにする）
@Controller
@RequestMapping("/admin/redeliveries")
public class AdminRedeliveryViewController {

  // 画面に出す再配達1件分の形（見本のデータ用の仮の形）
  // A010・A013 で本物の DTO を作ったら、この record は消して、そちらに置き換える
  public record SampleRedelivery(
      long id,// 再配達ID（詳細画面の URL に使う）
      String trackingNumber,// 配送番号
      LocalDate redeliveryDate,// 再配達の希望日
      String redeliveryTime,// 再配達の希望時間帯
      LocalDateTime requestedAt,// お客さんが依頼した日時
      LocalDateTime updatedAt,// 最後に変わった日時
      String previousDriverName,// 前回（不在だったとき）の担当
      Long assignedDriverId,// 割り振ったドライバー（まだなら null）
      String allocationStatus,// 割り振り状況（UNASSIGNED：未対応、DONE：割り振り済み）
      String deliveryStatus,// 配送物の状態（再配達を受け付けると PREPARING に戻る）
      String name,// お届け先の氏名
      String address,// お届け先の住所
      String callNumber,// 電話番号
      String email,// メールアドレス
      String itemType) {// 荷物の種類
  }

  // 見本のデータ。未対応を先に、依頼の古い順に並べている（A010 でもこの並びにする想定）
  private static final List<SampleRedelivery> SAMPLES = List.of(
      new SampleRedelivery(1L, "100000000005", LocalDate.of(2026, 11, 22), "18:00-20:00",
          LocalDateTime.of(2026, 11, 20, 9, 12), LocalDateTime.of(2026, 11, 20, 9, 12), "配送 二郎", null,
          "UNASSIGNED", "PREPARING", "田中 優", "東京都渋谷区道玄坂5-5-5", "08022220005", "tanaka@example.com", "NORMAL"),
      new SampleRedelivery(2L, "100000000003", LocalDate.of(2026, 11, 22), "午前中",
          LocalDateTime.of(2026, 11, 20, 11, 40), LocalDateTime.of(2026, 11, 20, 11, 40), "配送 三郎", null,
          "UNASSIGNED", "PREPARING", "鈴木 美咲", "神奈川県横浜市中区3-3-3", "08022220003", "suzuki@example.com", "FROZEN"),
      new SampleRedelivery(3L, "100000000002", LocalDate.of(2026, 11, 21), "14:00-16:00",
          LocalDateTime.of(2026, 11, 19, 18, 5), LocalDateTime.of(2026, 11, 20, 8, 30), "配送 二郎", 3L,
          "DONE", "PREPARING", "佐藤 健", "東京都新宿区西新宿2-2-2", "08022220002", "sato@example.com", "RAW FOOD"));

  private final DriverService driverService;

  public AdminRedeliveryViewController(DriverService driverService) {
    this.driverService = driverService;
  }

  // 再配達一覧の画面を表示する
  @GetMapping
  public String list(Model model) {
    model.addAttribute("redeliveries", SAMPLES);
    model.addAttribute("sampleData", true);// 画面に「準備中（見本のデータ）」と出すための印
    return "admin/redeliveries";// /WEB-INF/jsp/admin/redeliveries.jsp を表示する
  }

  // 再配達詳細の画面を表示する。例: /admin/redeliveries/1（URL は再配達の id。1つの配送物に再配達が何回もありうるため）
  @GetMapping("/{id}")
  public String detail(@PathVariable long id, Model model) {
    SampleRedelivery redelivery = SAMPLES.stream()
        .filter(r -> r.id() == id)
        .findFirst()
        .orElseThrow(() -> new NotFoundException("該当する再配達が見つかりません"));// 見つからなければ404の画面（A011 と同じ文言）

    model.addAttribute("redelivery", redelivery);
    model.addAttribute("drivers", driverService.findDrivers());// 割り振りのプルダウンは、本物のドライバー一覧を使う
    model.addAttribute("sampleData", true);
    return "admin/redeliveryDetail";// /WEB-INF/jsp/admin/redeliveryDetail.jsp を表示する
  }
}
