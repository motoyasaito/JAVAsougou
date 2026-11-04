package com.example.demo.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.AssignDriverRequest;
import com.example.demo.service.RedeliveryService;
import org.springframework.web.bind.annotation.RequestBody;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/redeliveries")
public class AdminRedeliveryController {
  // 実際の処理（チェックや更新）を任せるService。finalなので、一度入れたら差し替えられない
  private final RedeliveryService redeliveryService;

  // コンストラクタインジェクション：Springが作ったRedeliveryServiceを、ここで受け取ってフィールドに入れる
  public AdminRedeliveryController(RedeliveryService redeliveryService) {
    this.redeliveryService = redeliveryService;
  }

  // 再配達に担当ドライバーを割り振る
  // ResponseEntity<Void>：ステータスコードだけを返し、本文（body）は返さない
  // @PathVariable：URLの {id} の部分を、引数の id に入れる
  // @RequestBody：送られてきたJSON（例：{"driverId": 2}）を、AssignDriverRequestに変換して request
  // に入れる
  // @Valid：AssignDriverRequestに付けたチェック（driverIdが必須など）を実行する。違反なら400になり、このメソッドは動かない
  @PostMapping("/{id}/driver")
  public ResponseEntity<Void> reassignDriver(@PathVariable long id, @Valid @RequestBody AssignDriverRequest request) {
    // チェックと更新はServiceに任せる。404や409のときは、Serviceが投げた例外をGlobalExceptionHandlerが受け取ってレスポンスにする
    redeliveryService.reassignDriver(id, request);
    // ここまで来たら成功。ステータス200を、本文なしで返す
    return ResponseEntity.ok().build();
  }
}
