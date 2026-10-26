# コード雛形

既存コードと同じ形で新しい機能を書くための雛形。
例として「ドライバーが配送状況を更新する（`PATCH /api/driver/deliveries/{id}/status`）」機能を使う。

## 目次
1. pom.xml の依存関係
2. Mapper
3. DTO（record）
4. Service
5. 例外クラスと GlobalExceptionHandler への追加
6. Controller
7. Controller テスト

---

## 1. pom.xml の依存関係

すでに入っているもの。新しく追加するときはユーザーに確認する。

| 依存 | 用途 |
|---|---|
| `spring-boot-starter-webmvc` | REST API |
| `spring-boot-starter-validation` | 入力チェック |
| `spring-boot-starter-jdbc` / `postgresql` | DB 接続 |
| `mybatis-spring-boot-starter` | MyBatis |
| `spring-boot-starter-webmvc-test` など `*-test` | テスト |

`application.properties`:

```properties
mybatis.configuration.map-underscore-to-camel-case=true
```

## 2. Mapper

```java
package com.example.demo.mapper;

// 既存の DeliveryMapper に追加する

  // 配送物を1件取得する
  @Select("""
      SELECT id, tracking_number, name, address, call_number, email, status, item_type,
             driver_id, delivery_date, delivery_time, created_at, updated_at
      FROM deliveries
      WHERE id = #{id}
      """)
  Optional<Delivery> findById(@Param("id") long id);

  // 配送状況を更新する。戻り値は更新した件数（0なら該当なし）
  @Update("""
      UPDATE deliveries
      SET status = #{status}, updated_at = now()
      WHERE id = #{id}
      """)
  int updateStatus(@Param("id") long id, @Param("status") String status);
```

## 3. DTO（record）

リクエスト:

```java
package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

// 配送状況の更新で受け取る形
public record UpdateDeliveryStatusRequest(
    @NotBlank(message = "配送状況を指定してください")
    @Pattern(regexp = "PREPARING|IN TRANSIT|DELIVERED|ABSENT", message = "配送状況の値が正しくありません")
    String status) {
}
```

レスポンスは既存の `DeliveryResponse`（`from` で Entity から変換）と、一覧なら `PageResponse<T>` を使う。

## 4. Service

```java
package com.example.demo.service;

// 既存の DeliveryService に追加する

  // 配送状況を更新して、更新後の配送物を返す
  @Transactional
  public DeliveryResponse updateStatus(long id, String status) {
    int updated = deliveryMapper.updateStatus(id, status);
    if (updated == 0) {
      throw new ResourceNotFoundException("指定した配送物は存在しません");
    }
    Delivery delivery = deliveryMapper.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("指定した配送物は存在しません"));
    return DeliveryResponse.from(delivery);
  }
```

- `@Transactional` は `org.springframework.transaction.annotation.Transactional`。
- 見つからないときは `null` を返さず、例外を投げる。

## 5. 例外クラスと GlobalExceptionHandler への追加

例外クラス（`exception/` に置く）:

```java
package com.example.demo.exception;

// 指定したIDのデータが存在しないときに投げる（404になる）
public class ResourceNotFoundException extends RuntimeException {
  public ResourceNotFoundException(String message) {
    super(message);
  }
}
```

`GlobalExceptionHandler` に追加するハンドラ。最後の `Exception` ハンドラより上に書く:

```java
  // 自作の例外: データが存在しない
  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException e) {
    return build(HttpStatus.NOT_FOUND, e.getMessage(), List.of());
  }

  // パス・クエリパラメータの入力チェック（@Min・@Max など）に引っかかった時
  @ExceptionHandler(HandlerMethodValidationException.class)
  public ResponseEntity<ErrorResponse> handleMethodValidation(HandlerMethodValidationException e) {
    List<String> details = e.getParameterValidationResults().stream()
        .flatMap(r -> r.getResolvableErrors().stream()
            .map(err -> r.getMethodParameter().getParameterName() + ":" + err.getDefaultMessage()))
        .toList();
    return build(HttpStatus.BAD_REQUEST, "入力内容に誤りがあります", details);
  }

  // ?page=abc のように、パラメータの型が違う時
  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
    return build(HttpStatus.BAD_REQUEST, "入力内容に誤りがあります", List.of(e.getName() + ":値の形式が正しくありません"));
  }
```

import:

```java
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
```

## 6. Controller

```java
package com.example.demo.controller;

import com.example.demo.dto.DeliveryResponse;
import com.example.demo.dto.UpdateDeliveryStatusRequest;
import com.example.demo.service.DeliveryService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/driver/deliveries")
public class DriverDeliveryController {

  private final DeliveryService deliveryService;

  public DriverDeliveryController(DeliveryService deliveryService) {
    this.deliveryService = deliveryService;
  }

  // 配送状況を更新する
  @PatchMapping("/{id}/status")
  public DeliveryResponse updateStatus(
      @PathVariable long id,
      @Valid @RequestBody UpdateDeliveryStatusRequest request) {
    return deliveryService.updateStatus(id, request.status());
  }
}
```

- 200 で返すので `ResponseEntity` で包まず、DTO をそのまま返す。
- 作成（201）のときだけ `ResponseEntity.status(HttpStatus.CREATED).body(...)`、削除（204）のときは `ResponseEntity.noContent().build()` を使う。

## 7. Controller テスト

```java
package com.example.demo.controller;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.service.DeliveryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(DriverDeliveryController.class)
class DriverDeliveryControllerTest {

  @Autowired
  MockMvc mockMvc;

  @MockitoBean
  DeliveryService deliveryService;

  @Test
  void 配送状況が不正な値なら400を返す() throws Exception {
    mockMvc.perform(patch("/api/driver/deliveries/1/status")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"status": "UNKNOWN"}
                """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.details[0]").value(org.hamcrest.Matchers.startsWith("status:")));
  }

  @Test
  void 存在しない配送物なら404を返す() throws Exception {
    // Given
    when(deliveryService.updateStatus(anyLong(), anyString()))
        .thenThrow(new ResourceNotFoundException("指定した配送物は存在しません"));

    // When / Then
    mockMvc.perform(patch("/api/driver/deliveries/999/status")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"status": "DELIVERED"}
                """))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404));
  }
}
```

- Spring Boot 4 では `@WebMvcTest` のパッケージが `org.springframework.boot.webmvc.test.autoconfigure` に変わっている（Boot 3 の `org.springframework.boot.test.autoconfigure.web.servlet` ではない）。
