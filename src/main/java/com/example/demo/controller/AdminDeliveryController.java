package com.example.demo.controller;

import com.example.demo.dto.CreateDeliveryRequest;
import com.example.demo.dto.DeliveryResponse;
import com.example.demo.dto.PageResponse;
import com.example.demo.service.DeliveryService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController // このクラスはAPIの窓口。メソッドの戻り値は、自動でJSONに変換されてレスポンスになる
@RequestMapping("/api/admin/deliveries") // このクラスのAPIはすべて /api/admin/deliveries から始まる
public class AdminDeliveryController {

  private final DeliveryService deliveryService;// 業務の処理を任せるService。finalなので、一度入れたら差し替えられない

  public AdminDeliveryController(DeliveryService deliveryService) {// Springが起動時にServiceを作って、ここに渡してくれる（コンストラクタインジェクション）
    this.deliveryService = deliveryService;// 受け取ったServiceをフィールドに入れて、下のメソッドで使えるようにする
  }

  // 配送物の一覧を取得する
  // TODO:認証処理に関しては別チケットで対応（ログインユーザーのロールを確認し、管理者のみ呼べるようにする）
  @GetMapping // GET /api/admin/deliveries で呼ばれる
  public PageResponse<DeliveryResponse> findAll(
      @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size, // URLの ?size= を受け取る。省略したら10。1〜100以外は400になる
      @RequestParam(defaultValue = "0") @Min(0) int page) {// URLの ?page= を受け取る。省略したら0。マイナスは400になる
    return deliveryService.findAll(page, size);// Serviceに取得を任せて、結果をそのまま返す（ステータスは自動で200）
  }

  // 配送物を登録する。登録出来たら201と登録した内容を返す
  // TODO:認証処理に関しては別チケットで対応（管理者のみ登録できるようにする）
  @PostMapping // POST /api/admin/deliveries で呼ばれる
  // @RequestBody：リクエストの本文（JSON）をCreateDeliveryRequestに変換して受け取る
  // @Valid：CreateDeliveryRequestに付けた入力チェック（@NotBlankなど）を実行する。引っかかったら400になる
  // ResponseEntity：ステータスコード（201）を自分で指定して返すための入れ物
  public ResponseEntity<DeliveryResponse> create(@RequestBody @Valid CreateDeliveryRequest request) {
    DeliveryResponse created = deliveryService.create(request);// Serviceに登録を任せて、登録した内容を受け取る
    URI location = URI.create("/api/admin/deliveries/" + created.trackingNumber());// 登録したデータの場所（例:/api/admin/deliveries/123456789012）を作る。詳細APIは伝票番号で取得するため
    return ResponseEntity.created(location).body(created);// created()でステータス201とLocationヘッダーを付け、body()で本文に登録した内容を入れて返す
  }

  // 伝票番号で配送物の詳細を取得する
  // TODO:認証処理に関しては別チケットで対応（管理者のみ呼べるようにする）
  @GetMapping("/{trackingNumber}") // GET /api/admin/deliveries/123456789012 のように、URLの最後に伝票番号を付けて呼ばれる
  // @PathVariable：URLの {trackingNumber} の部分を、同じ名前の引数で受け取る
  // @Pattern：12桁の数字でなければ400になる（一覧の@Min・@Maxと同じく、引数に直接付けられる）
  public DeliveryResponse findByTrackingNumber(
      @PathVariable @Pattern(regexp = "^[0-9]{12}$", message = "伝票番号は12桁の数字で入力してください") String trackingNumber) {
    return deliveryService.findByTrackingNumber(trackingNumber);// Serviceに取得を任せて、結果をそのまま返す（ステータスは自動で200）
  }
}
