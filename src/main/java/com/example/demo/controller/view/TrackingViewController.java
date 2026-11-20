package com.example.demo.controller.view;

import com.example.demo.dto.DeliveryRules;
import com.example.demo.dto.DeliveryStatusResponse;
import com.example.demo.exception.NotFoundException;
import com.example.demo.service.DeliveryService;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

// お客さん用の配送状況確認の「画面」を受け持つController。ログインなしで使える
// 管理者の画面（/admin）と分けて /tracking にしておくと、ログインを作るときに「/admin だけ守る」と設定しやすい
@Controller
@RequestMapping("/tracking")
public class TrackingViewController {

  private final DeliveryService deliveryService;

  public TrackingViewController(DeliveryService deliveryService) {
    this.deliveryService = deliveryService;
  }

  // 配送番号の入力画面を表示する
  @GetMapping
  public String input() {
    return "customer/tracking";// /WEB-INF/jsp/customer/tracking.jsp を表示する
  }

  // 配送状況を表示する。入力画面の「検索」で /tracking/status?trackingNumber=... が開かれる
  @GetMapping("/status")
  public String status(@RequestParam(defaultValue = "") String trackingNumber, Model model,
      RedirectAttributes redirectAttributes) {
    String input = trackingNumber.strip();// 前後の空白を取る（コピーしたときに空白が付いてくることがあるため）

    if (!DeliveryRules.isTrackingNumber(input)) {// 伝票番号の形（数字12桁）は DeliveryRules に1か所でまとめている
      return backToInput(redirectAttributes, input, "配送番号は12桁の数字で入力してください");
    }

    // 見つからなかったときは、エラー画面にせず、入力画面に戻ってメッセージを出す（入力し直してもらうため）
    // 例外を握りつぶすのではなく、お客さんに分かる形で伝えるために受け止めている
    DeliveryStatusResponse delivery;
    try {
      delivery = deliveryService.findStatusByTrackingNumber(input);// API（A008）と同じServiceのメソッド。個人情報は返さない
    } catch (NotFoundException e) {
      return backToInput(redirectAttributes, input, e.getMessage());
    }

    model.addAttribute("delivery", delivery);
    return "customer/trackingStatus";// /WEB-INF/jsp/customer/trackingStatus.jsp を表示する
  }

  // 入力画面に戻して、メッセージと入力した値を1回だけ渡す
  private String backToInput(RedirectAttributes redirectAttributes, String input, String message) {
    redirectAttributes.addFlashAttribute("errorMessage", message);
    redirectAttributes.addFlashAttribute("trackingNumber", input);
    return "redirect:/tracking";
  }
}
