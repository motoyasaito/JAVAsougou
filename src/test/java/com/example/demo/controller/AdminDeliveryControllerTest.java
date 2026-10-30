package com.example.demo.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.demo.dto.CreateDeliveryRequest;
import com.example.demo.dto.DeliveryResponse;
import com.example.demo.dto.PageResponse;
import com.example.demo.exception.NotFoundException;
import com.example.demo.service.DeliveryService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

// Service をモックにして、パラメータの受け取り方・ステータス・JSON の形を確認する
@WebMvcTest(AdminDeliveryController.class) // 指定したコントローラーだけで、springのweb部分を起動。DBやマッパーは起動しない。
class AdminDeliveryControllerTest {

  @Autowired
  MockMvc mockMvc;// MockMvcはテスト用のPostマン。実際にサーバーを立てずにコントローラーにリクエストを送れる。

  @MockitoBean
  DeliveryService deliveryService;// サービスの偽物を作ってコントローラーに返す。

  @Test
  void ページと件数を指定すると配送物の一覧を返す() throws Exception {
    // Given 偽物サービスに「findAll(1．5)と呼ばれたら1件入った結果を返す（全6件）」と教える
    DeliveryResponse delivery = new DeliveryResponse(1L, "123456789012", "山田太郎", "東京都千代田区1-1",
        "09012345678", "taro@example.com", "PREPARING", "NORMAL", 2L, LocalDate.of(2026, 10, 23), "午前中");
    when(deliveryService.findAll(1, 5)).thenReturn(PageResponse.of(List.of(delivery), 1, 5, 6L));

    // When / Then
    // When: GET /api/admin/deliveries?page=1&size=5 を送ります。Postman の Params
    // 欄に入力するのと同じです。
    // ここで大事なのは、コントローラーが URL の "1" と "5"（文字）を int の 1 と 5 に変えて、正しい順番でサービスに渡せるか。もし
    // page と size を逆に渡していたら、偽物は何も返さず、下の確認で失敗する。
    mockMvc.perform(get("/api/admin/deliveries")
        .param("page", "1")
        .param("size", "5"))
        .andExpect(status().isOk())// ステータスが200か確かめる
        .andExpect(jsonPath("$.content[0].trackingNumber").value("123456789012"))// 返ってきたJsonの中身を確かめる。$ が JSON
                                                                                 // 全体で、.content[0] は「content
                                                                                 // の1件目」、.trackingNumber はその中の項目
        .andExpect(jsonPath("$.content[0].deliveryDate").value("2026-10-23"))
        .andExpect(jsonPath("$.page").value(1))
        .andExpect(jsonPath("$.size").value(5))
        .andExpect(jsonPath("$.totalElements").value(6))
        .andExpect(jsonPath("$.totalPages").value(2));
  }

  @Test
  void ページ指定を省略したら既定値でServiceを呼ぶ() throws Exception {
    // Given
    when(deliveryService.findAll(0, 10)).thenReturn(PageResponse.of(List.of(), 0, 10, 0L));

    // When パラメータなしで送ったときに、defaultValue の 0 と 10 でサービスが呼ばれるかを確かめる。もし誰かが defaultValue
    // を書き換えたら、ここで気づける。
    mockMvc.perform(get("/api/admin/deliveries"))
        .andExpect(status().isOk());

    // Then
    verify(deliveryService).findAll(0, 10);
  }

  @Test
  void sizeが上限の100なら取得できる() throws Exception {
    // Given ＠Max（100）は100以下ならOKという意味。ちょうど100が通るかを確かめる。
    when(deliveryService.findAll(0, 100)).thenReturn(PageResponse.of(List.of(), 0, 100, 0L));

    // When / Then
    mockMvc.perform(get("/api/admin/deliveries").param("size", "100"))
        .andExpect(status().isOk());
  }

  @Test
  // isBadRequest() は「400 か」を確かめる。ここではモックに何も教えていない。入力チェックで止まるので、サービスまで届かないから。
  void sizeが0なら400を返す() throws Exception {
    mockMvc.perform(get("/api/admin/deliveries").param("size", "0"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void sizeが101なら400を返す() throws Exception {
    mockMvc.perform(get("/api/admin/deliveries").param("size", "101"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void pageが負の数なら400を返す() throws Exception {
    mockMvc.perform(get("/api/admin/deliveries").param("page", "-1"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void sizeが数字でなければ400を返す() throws Exception {
    mockMvc.perform(get("/api/admin/deliveries").param("size", "abc"))
        .andExpect(status().isBadRequest());
  }

  // ---------- 配送物の登録（POST） ----------

  @Test
  void 登録できたら201とLocationと登録した内容を返す() throws Exception {
    // Given 偽物サービスに「どんなリクエストで呼ばれても、この登録結果を返す」と教える
    DeliveryResponse created = new DeliveryResponse(1L, "123456789012", "山田太郎", "東京都千代田区1-1",
        "09012345678", "taro@example.com", "PREPARING", "NORMAL", null, LocalDate.of(2026, 10, 28), "午前中");
    when(deliveryService.create(any(CreateDeliveryRequest.class))).thenReturn(created);// any()は「どんなリクエストが来ても」という意味

    // When / Then
    mockMvc.perform(post("/api/admin/deliveries")// get()のPOST版
        .contentType(MediaType.APPLICATION_JSON)// 「送るのはJSONです」と伝える。PostmanのBodyでraw→JSONを選ぶのと同じ
        .content(validJson()))// 送るJSONの本文。PostmanのBodyに貼る内容と同じ
        .andExpect(status().isCreated())// ステータスが201か確かめる
        .andExpect(header().string("Location", "/api/admin/deliveries/123456789012"))// PostmanのHeadersタブで見たLocationの値を確かめる
        .andExpect(jsonPath("$.trackingNumber").value("123456789012"))
        .andExpect(jsonPath("$.status").value("PREPARING"));
  }

  @Test
  void 氏名が空なら400を返す() throws Exception {
    // 正しいJSONの氏名の部分だけを空文字に書き換えて送る
    mockMvc.perform(post("/api/admin/deliveries")
        .contentType(MediaType.APPLICATION_JSON)
        .content(validJson().replace("\"name\": \"山田太郎\"", "\"name\": \"\"")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.details[0]").value("name:お届け先の氏名を入力してください"));// detailsの1件目。GlobalExceptionHandlerが「項目名:メッセージ」の形で作る

    verify(deliveryService, never()).create(any());// never()は「一度も呼ばれていない」。入力チェックで止まり、Serviceまで届いていないことを確かめる
  }

  @Test
  void 荷物の種類が選択肢になければ400を返す() throws Exception {
    mockMvc.perform(post("/api/admin/deliveries")
        .contentType(MediaType.APPLICATION_JSON)
        .content(validJson().replace("NORMAL", "BOOK")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.details[0]").value("itemType:荷物の種類の指定が正しくありません"));
  }

  @Test
  void 配達日の形式が正しくなければ400を返す() throws Exception {
    // 日付の形式が違うとJSONを読み込めず、handleNotReadableで400になる。このハンドラはdetailsが空なのでstatusだけ確かめる
    mockMvc.perform(post("/api/admin/deliveries")
        .contentType(MediaType.APPLICATION_JSON)
        .content(validJson().replace("2026-10-28", "2026/10/28")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400));
  }

  // ---------- 配送物の詳細（GET /{trackingNumber}） ----------

  @Test
  void 伝票番号を指定すると配送物の詳細を返す() throws Exception {
    // Given 偽物サービスに「この伝票番号で呼ばれたら、この配送物を返す」と教える
    DeliveryResponse delivery = new DeliveryResponse(1L, "123456789012", "山田太郎", "東京都千代田区1-1",
        "09012345678", "taro@example.com", "PREPARING", "NORMAL", 2L, LocalDate.of(2026, 10, 28), "午前中");// 返す配送物を1件作る
    when(deliveryService.findByTrackingNumber("123456789012")).thenReturn(delivery);// URLの伝票番号がそのままServiceに渡されれば、この配送物が返る

    // When / Then
    mockMvc.perform(get("/api/admin/deliveries/123456789012"))// URLの最後に伝票番号を付けて送る。PostmanのURL欄に書くのと同じ
        .andExpect(status().isOk())// ステータスが200か確かめる
        .andExpect(jsonPath("$.trackingNumber").value("123456789012"))// 返ってきたJSONの伝票番号を確かめる
        .andExpect(jsonPath("$.name").value("山田太郎"))// 氏名を確かめる
        .andExpect(jsonPath("$.driverId").value(2));// 一覧にはない確認として、担当ドライバーのidも確かめる
  }

  @Test
  void 伝票番号が見つからなければ404を返す() throws Exception {
    // Given 偽物サービスに「この伝票番号で呼ばれたら、NotFoundExceptionを投げる」と教える
    when(deliveryService.findByTrackingNumber("999999999999"))
        .thenThrow(new NotFoundException("該当する配送物が見つかりません"));// thenThrowは「値を返す」ではなく「例外を投げる」

    // When / Then
    mockMvc.perform(get("/api/admin/deliveries/999999999999"))
        .andExpect(status().isNotFound())// GlobalExceptionHandlerが404に変えているか確かめる
        .andExpect(jsonPath("$.message").value("該当する配送物が見つかりません"));// 例外のメッセージがそのままレスポンスに入っているか
  }

  @Test
  void 伝票番号が12桁でなければ400を返す() throws Exception {
    // 12桁でない番号を送ると、@Patternで止まりServiceまで届かない。なのでGivenはいらない
    mockMvc.perform(get("/api/admin/deliveries/12345"))
        .andExpect(status().isBadRequest())// ステータスが400か確かめる
        .andExpect(jsonPath("$.details[0]").value("trackingNumber:伝票番号は12桁の数字で入力してください"));// handleParameterValidationが「引数名:メッセージ」の形で作る

    verify(deliveryService, never()).findByTrackingNumber(any());// 入力チェックで止まり、Serviceが一度も呼ばれていないことを確かめる
  }

  // テストで送る正しい登録リクエストのJSON。"""で囲むと複数行の文字列をそのまま書ける（MapperのSQLと同じ書き方）
  private String validJson() {
    return """
        {
          "name": "山田太郎",
          "address": "東京都千代田区1-1",
          "callNumber": "09012345678",
          "email": "taro@example.com",
          "itemType": "NORMAL",
          "deliveryDate": "2026-10-28",
          "deliveryTime": "午前中"
        }
        """;
  }
}
