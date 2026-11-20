package com.example.demo.controller.view;

import com.example.demo.dto.DeliveryResponse;
import com.example.demo.dto.PageResponse;
import com.example.demo.service.DeliveryService;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

// 管理者の配送物一覧の「画面」を返すController
// @RestController（JSONを返す）ではなく @Controller にすると、戻り値の文字列が「表示するJSPの名前」になる
// TODO: 認証処理は別チケットで対応（管理者のみ開けるようにする）
@Controller
@RequestMapping("/admin/deliveries") // 画面のURLは /api を付けない。API（/api/admin/deliveries）と分けるため
public class AdminDeliveryViewController {

  // 画面の1ページの件数。画面では件数を選ばせないので固定にする
  private static final int PAGE_SIZE = 10;

  // 伝票番号の形（数字12桁）。APIの入力チェックと同じ決まり
  private static final String TRACKING_NUMBER_PATTERN = "^[0-9]{12}$";

  private final DeliveryService deliveryService;// APIと同じServiceを使う。業務のルールは1か所にまとめたまま

  public AdminDeliveryViewController(DeliveryService deliveryService) {
    this.deliveryService = deliveryService;
  }

  // 配送物一覧の画面を表示する。例: /admin/deliveries?page=1 で2ページ目
  @GetMapping
  public String list(@RequestParam(defaultValue = "0") int page, Model model) {// Model は「JSPに渡す荷物入れ」。ここに入れた値をJSPで使える
    // 画面ではURLを手で書き換えられるので、マイナスのページは400にせず1ページ目を表示する
    int currentPage = Math.max(page, 0);// Math.max(a, b) は大きいほうを返す。-1 なら 0 になる

    PageResponse<DeliveryResponse> result = deliveryService.findAll(currentPage, PAGE_SIZE);// APIのControllerと同じメソッドを直接呼ぶ
    model.addAttribute("result", result);// "result" という名前でJSPに渡す。JSPでは ${result.content} のように使う

    return "admin/deliveries";// /WEB-INF/jsp/admin/deliveries.jsp を表示する（application.properties の prefix と suffix が付く）
  }

  // 伝票番号で検索する。見つかればその配送物の詳細画面へ、見つからなければ一覧に戻してメッセージを出す
  @GetMapping("/search")
  public String search(@RequestParam(defaultValue = "") String trackingNumber,
      RedirectAttributes redirectAttributes) {// RedirectAttributes は「画面を移ったあとに1回だけ渡したい値」を入れる入れ物
    String input = trackingNumber.strip();// 前後の空白を取る（コピーしたときに空白が付いてくることがあるため）

    // 形が違う、または見つからないときは、一覧に戻してメッセージを出す
    String errorMessage = null;
    if (!input.matches(TRACKING_NUMBER_PATTERN)) {// matches は「文字列が正規表現の形に合っているか」を調べる
      errorMessage = "伝票番号は12桁の数字で入力してください";
    } else if (!deliveryService.existsByTrackingNumber(input)) {
      errorMessage = "該当する配送物が見つかりません";
    }

    if (errorMessage != null) {
      redirectAttributes.addFlashAttribute("searchError", errorMessage);// フラッシュ属性：移った先の画面で1回だけ使える値
      redirectAttributes.addFlashAttribute("trackingNumber", input);// 入力した値を検索欄に残す
      return "redirect:/admin/deliveries";// "redirect:" を付けると、JSPを表示するのではなく、ブラウザにそのURLを開き直させる
    }

    // 詳細画面へ移る（詳細画面はこのあと作る）
    return "redirect:/admin/deliveries/" + input;
  }
}
