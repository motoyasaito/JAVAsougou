package com.example.demo.controller.view;

import com.example.demo.dto.CreateDeliveryRequest;
import com.example.demo.dto.DeliveryResponse;
import com.example.demo.service.DeliveryService;

import jakarta.validation.Valid;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

// 管理者の荷物登録の「画面」を受け持つController。入力画面の表示と、登録の受け付け
// TODO: 認証処理は別チケットで対応（管理者のみ開けるようにする）
@Controller
@RequestMapping("/admin/deliveries")
public class AdminDeliveryNewViewController {

  private final DeliveryService deliveryService;

  public AdminDeliveryNewViewController(DeliveryService deliveryService) {
    this.deliveryService = deliveryService;
  }

  // 登録画面を表示する（一覧の「＋ 荷物を登録」から来る）
  @GetMapping("/new")
  public String newForm() {
    return "admin/deliveryNew";// /WEB-INF/jsp/admin/deliveryNew.jsp を表示する
  }

  // 登録画面の「登録」ボタン（POST /admin/deliveries）で呼ばれる
  @PostMapping
  public String create(
      // @ModelAttribute：フォームから送られた値（name=... など）を、CreateDeliveryRequest の同じ名前の項目に入れる
      // @Valid：CreateDeliveryRequest に付いている入力チェック（@NotBlank など）を、APIと同じルールで行う
      @Valid @ModelAttribute("form") CreateDeliveryRequest form,
      // BindingResult：入力チェックの結果。@Valid のすぐ後ろに書くと、間違いがあっても例外にならず、ここに結果が入る
      BindingResult bindingResult,
      Model model,
      RedirectAttributes redirectAttributes) {

    if (bindingResult.hasErrors()) {
      // 間違いがあれば登録せずに、同じ登録画面をもう一度表示する。項目ごとのメッセージを渡す
      // ここはリダイレクトしない。リダイレクトすると、入力した値が消えてしまうため
      model.addAttribute("fieldErrors", FieldErrors.from(bindingResult));// 項目ごとのメッセージの形にする（FieldErrors は画面で共通の道具）
      return "admin/deliveryNew";
    }

    DeliveryResponse created = deliveryService.create(form);// APIと同じServiceで登録する。伝票番号もここで発行される

    // 登録できたら、その配送物の詳細画面へ移る。続けてドライバーを割り当てられるようにするため
    redirectAttributes.addFlashAttribute("successMessage",
        "荷物を登録しました（伝票番号：" + created.trackingNumber() + "）");
    return "redirect:/admin/deliveries/" + created.trackingNumber();// リダイレクトして、再読み込みで二重に登録されないようにする（PRGパターン）
  }
}
