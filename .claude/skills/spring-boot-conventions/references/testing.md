# テスト 規約

## 目次
1. 現状と使うライブラリ
2. テストの種類と使い分け
3. 書き方の共通ルール
4. Service の単体テスト
5. Controller のテスト（@WebMvcTest）
6. Mapper のテスト
7. Maven での実行

---

## 1. 現状と使うライブラリ

現状、テストは `DemoApplicationTests`（起動確認）だけ。新しく書く Service・Controller からテストを足していく。

`pom.xml` にある次のテスト用ライブラリを使う（Spring Boot 4 では機能ごとにテスト用 starter が分かれている）。

- `spring-boot-starter-webmvc-test`：`@WebMvcTest`、MockMvc
- `spring-boot-starter-validation-test`
- `spring-boot-starter-jdbc-test`

JUnit 5、AssertJ、Mockito はこれらに含まれる。
アサーションは **AssertJ（`assertThat`）に統一**する。JUnit の `assertEquals` と混在させない。

## 2. テストの種類と使い分け

速いテストを多く、遅いテストを少なく。

| 対象 | 方法 | Spring 起動 | DB | 主に確認すること |
|---|---|---|---|---|
| Service | JUnit + Mockito | しない | 不要 | 業務ロジック、ページング計算、例外を投げる条件 |
| Controller | `@WebMvcTest` | Web層のみ | 不要 | ステータス、JSON の形、入力チェック、エラー形式 |
| Mapper | （方針未決。6 を参照） | — | 必要 | SQL の WHERE・ORDER BY・件数 |

- Service の単体テストで Spring を起動しない。`@SpringBootTest` を何にでも使うと、テストが遅くなり、DB がないと動かなくなる。
- 優先順位は **Service → Controller**。Mapper のテストは環境が決まってから。

## 3. 書き方の共通ルール

- テストクラスは `src/test/java/com/example/demo/` の下に、本体と同じパッケージで作る（`service/DeliveryServiceTest.java`）。
- テストメソッド名は **日本語で「条件と期待結果」** を書く: `存在しないIDなら404を返す()`。
- 本体は **Given / When / Then** の3ブロックに分け、コメントで区切る。
- 1テストで確認する振る舞いは1つ。
- 正常系だけでなく、境界値（`page=0`、`size=1`、`size=100`、`size=101`）と異常系を書く。
- テスト間で状態を共有しない。実行順序に依存させない。

## 4. Service の単体テスト

Mapper を Mockito でモックし、Service のロジックだけを確認する。

```java
package com.example.demo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.demo.dto.DeliveryResponse;
import com.example.demo.dto.PageResponse;
import com.example.demo.entity.Delivery;
import com.example.demo.mapper.DeliveryMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeliveryServiceTest {

  @Mock
  DeliveryMapper deliveryMapper;

  @InjectMocks
  DeliveryService deliveryService;

  @Test
  void ページ番号と件数からoffsetを計算してMapperに渡す() {
    // Given
    when(deliveryMapper.findAll(10, 20)).thenReturn(List.of(new Delivery()));
    when(deliveryMapper.count()).thenReturn(25L);

    // When
    PageResponse<DeliveryResponse> result = deliveryService.findAll(2, 10);

    // Then
    verify(deliveryMapper).findAll(10, 20);
    assertThat(result.totalPages()).isEqualTo(3);
  }
}
```

## 5. Controller のテスト（@WebMvcTest）

- `@WebMvcTest(XxxController.class)` で対象を絞る。`@RestControllerAdvice` も自動で読み込まれるので、`ErrorResponse` の形もここで確認できる。
- Service は `@MockitoBean` でモックにする。
- 各エンドポイントで最低限: 正常系 / 入力チェックのエラー（400）/ 存在しない（404。1件取得・更新のとき）。
- エラー時は `status` と `details` を確認し、`message` の文言は確認しなくてよい（変わりやすいため）。
- コード例は `rest-api-templates.md` の「7. Controller テスト」を参照。

## 6. Mapper のテスト

SQL の間違い（WHERE の付け忘れ、件数の条件のずれ）は Mapper のテストでしか見つからないが、本物の PostgreSQL が必要になる。どう用意するか（Testcontainers、ローカルの DB、`mybatis-spring-boot-starter-test` の `@MybatisTest` など）は**まだ決まっていない**。

- Mapper のテストを書く前に、DB の用意の仕方をユーザーに確認する。
- 決まるまでは、SQL を書いたらアプリを起動して実際に API を呼び、結果を目で確認する。
- H2 などの別の DB は使わない（PostgreSQL と SQL の書き方や制約の挙動が違い、「テストは通るのに本番で落ちる」原因になるため）。

## 7. Maven での実行

```bash
./mvnw test     # すべてのテストを実行（Windows は mvnw.cmd test）
./mvnw test -Dtest=DeliveryServiceTest   # 1クラスだけ実行
```

- `DemoApplicationTests` はアプリ全体を起動するため、PostgreSQL が起動していないと失敗する。
