package com.example.demo.controller.view;

import com.example.demo.dto.CreateRedeliveryRequest;
import com.example.demo.dto.RedeliveryResponse;
import com.example.demo.exception.BadRequestException;
import com.example.demo.exception.ConflictException;
import com.example.demo.exception.NotFoundException;
import com.example.demo.service.RedeliveryService;

import jakarta.validation.Valid;
import java.time.Clock;
import java.time.LocalDate;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

// お客さん用の再配達依頼の「画面」を受け持つController。ログインなしで使える
@Controller
@RequestMapping("/redelivery")
public class RedeliveryRequestViewController {

  private final RedeliveryService redeliveryService;
  private final Clock clock;// 「今」を知る時計（TimeConfig で日本時間にしている）

  public RedeliveryRequestViewController(RedeliveryService redeliveryService, Clock clock) {
    this.redeliveryService = redeliveryService;
    this.clock = clock;
  }

  // 依頼の入力画面を表示する。配送状況の画面から来たときは ?trackingNumber=... が付いていて、最初から入力しておく
  // 受け付けたあとは、完了の情報（completed）を持ってこの画面に戻り、完了のモーダルを出す
  @GetMapping
  public String input(Model model) {
    addMinDate(model);
    return "customer/redelivery";// /WEB-INF/jsp/customer/redelivery.jsp を表示する
  }

  // 入力画面の「送信」（POST /redelivery）で呼ばれる
  @PostMapping
  public String create(@Valid @ModelAttribute("form") CreateRedeliveryRequest form,// APIと同じ入力チェック
      BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {

    if (bindingResult.hasErrors()) {
      // 間違いがあれば、同じ画面をもう一度表示する（リダイレクトしないので、入力した値が残る）
      model.addAttribute("fieldErrors", FieldErrors.from(bindingResult));
      addMinDate(model);
      return "customer/redelivery";
    }

    // Service の業務ルールで断られたとき（番号と電話番号が合わない、明日より前の日付、不在でない）は、
    // エラー画面にせず、入力画面の上に理由を出す。例外を握りつぶすのではなく、お客さんに分かる形で伝えるために受け止めている
    RedeliveryResponse created;
    try {
      created = redeliveryService.create(form);// API（A009）と同じServiceで登録する。配送物の状態は発送準備中に戻る
    } catch (NotFoundException | BadRequestException | ConflictException e) {
      model.addAttribute("errorMessage", e.getMessage());
      addMinDate(model);
      return "customer/redelivery";
    }

    // 受け付けたら、入力画面へリダイレクトして完了のモーダルを出す。再読み込みで二重に依頼されないようにするため（PRGパターン）
    redirectAttributes.addFlashAttribute("completed", created);
    return "redirect:/redelivery";
  }

  // 希望日として選べるいちばん早い日（明日）を JSP に渡す。カレンダーで今日以前を選べないようにするため
  // サーバー側（Service）でも同じチェックをしているので、ブラウザのチェックをすり抜けても受け付けない
  private void addMinDate(Model model) {
    model.addAttribute("minDate", LocalDate.now(clock).plusDays(1));// 日本時間の明日
  }
}
