package com.example.demo.controller.view;

import com.example.demo.dto.DeliveryRules;
import com.example.demo.dto.AssignDriverRequest;
import com.example.demo.dto.DeliveryDetailResponse;
import com.example.demo.dto.DeliveryResponse;
import com.example.demo.exception.ConflictException;
import com.example.demo.exception.NotFoundException;
import com.example.demo.service.DeliveryService;
import com.example.demo.service.DriverService;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

// 管理者の配送物詳細の「画面」を返すController。詳細の表示と、ドライバーの割り当てを受け持つ
// TODO: 認証処理は別チケットで対応（管理者のみ開けるようにする）
@Controller
@RequestMapping("/admin/deliveries/{trackingNumber}") // URLの {trackingNumber} の部分を、下のメソッドで @PathVariable として受け取る
public class AdminDeliveryDetailViewController {

  private final DeliveryService deliveryService;
  private final DriverService driverService;

  public AdminDeliveryDetailViewController(DeliveryService deliveryService, DriverService driverService) {
    this.deliveryService = deliveryService;
    this.driverService = driverService;
  }

  // 詳細画面を表示する。例: /admin/deliveries/100000000001
  @GetMapping
  public String detail(@PathVariable String trackingNumber, Model model) {
    checkTrackingNumber(trackingNumber);
    // 見つからなければ NotFoundException → ViewExceptionHandler が404の画面にする
    DeliveryDetailResponse detail = deliveryService.findDetailByTrackingNumber(trackingNumber);

    model.addAttribute("delivery", detail.delivery());// 伝票番号・氏名・状態など、いつもの項目
    model.addAttribute("detail", detail);// 登録日時・最終更新日時を出すのに使う
    model.addAttribute("drivers", driverService.findDrivers());// 割り当てのプルダウンと、今の担当者の名前を出すのに使う
    return "admin/deliveryDetail";// /WEB-INF/jsp/admin/deliveryDetail.jsp を表示する
  }

  // ドライバーを割り当てる（詳細画面の「割り振る」ボタン）
  // 終わったら同じ詳細画面を開き直させて、結果のメッセージを出す
  @PostMapping("/driver")
  public String assignDriver(@PathVariable String trackingNumber,
      @RequestParam(required = false) Long driverId,// プルダウンで選んだドライバー。何も選ばずに押すと null になる
      RedirectAttributes redirectAttributes) {
    DeliveryResponse delivery = findDelivery(trackingNumber);
    String detailUrl = "redirect:/admin/deliveries/" + delivery.trackingNumber();

    if (driverId == null) {
      redirectAttributes.addFlashAttribute("errorMessage", "ドライバーを選択してください");
      return detailUrl;
    }

    // Service の業務ルールで断られたとき（発送準備中でない、ドライバーがいない）は、
    // エラー画面にせず、詳細画面に戻ってその理由をメッセージで見せる。
    // 例外を握りつぶすのではなく、利用者に分かる形で伝えるために受け止めている
    try {
      deliveryService.assignDriver(delivery.id(), new AssignDriverRequest(driverId));// APIと同じServiceのメソッドを使う
    } catch (ConflictException | NotFoundException e) {// | で「どちらかの例外なら」の意味
      redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());// Service で決めたメッセージをそのまま出す
      return detailUrl;
    }

    redirectAttributes.addFlashAttribute("successMessage", "ドライバーを割り当てました");
    return detailUrl;// 登録・更新のあとはリダイレクトする。再読み込みで同じ割り当てが2回送られないようにするため（PRGパターン）
  }

  // 伝票番号で配送物を探す。形が違うときも、見つからないときと同じく404にする
  private DeliveryResponse findDelivery(String trackingNumber) {
    checkTrackingNumber(trackingNumber);
    return deliveryService.findByTrackingNumber(trackingNumber);
  }

  // 伝票番号が12桁の数字でなければ、DBを探さずに404にする
  private void checkTrackingNumber(String trackingNumber) {
    if (!DeliveryRules.isTrackingNumber(trackingNumber)) {// 伝票番号の形（数字12桁）は DeliveryRules に1か所でまとめている
      throw new NotFoundException("該当する配送物が見つかりません");
    }
  }
}
