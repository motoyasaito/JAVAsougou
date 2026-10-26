# REST API・例外処理 規約

コード雛形は `rest-api-templates.md` にある。新しい Controller・例外・ハンドラを書くときは併せて読むこと。

## 1. URL とHTTPメソッド

- ベースパスは `/api/v1`。バージョンをURLに含めることで、破壊的変更時に v2 を並行稼働できる。
- リソース名は複数形の名詞・ケバブケース（`/api/v1/order-items`）。動詞はURLに入れず、HTTPメソッドで表す。
- 対応表:

| 操作 | メソッド | パス | 成功時ステータス |
|---|---|---|---|
| 一覧取得 | GET | `/api/v1/users` | 200 |
| 1件取得 | GET | `/api/v1/users/{id}` | 200 |
| 作成 | POST | `/api/v1/users` | 201 + `Location` ヘッダ |
| 全体更新 | PUT | `/api/v1/users/{id}` | 200 |
| 部分更新 | PATCH | `/api/v1/users/{id}` | 200 |
| 削除 | DELETE | `/api/v1/users/{id}` | 204（ボディなし） |

- JSON のプロパティ名は camelCase（Jackson のデフォルトのまま）。日時は ISO-8601（`OffsetDateTime` または `Instant`）。

## 2. Controller の責務

Controller は「HTTP の変換係」に徹する。理由は、業務ロジックが Controller に混ざるとテストもしにくく、例外処理の流れも読みにくくなるから。

- `@RestController` + `@RequestMapping("/api/v1/...")` を付ける。
- 入力の受け取り（`@Valid @RequestBody`、`@PathVariable`、`@RequestParam`）→ Service 呼び出し → `ResponseEntity` で返す、だけを書く。
- **Controller に try-catch を書かない。** 例外は Service から投げっぱなしにし、`GlobalExceptionHandler` に一元的に任せる。
- コンストラクタインジェクション（`private final` フィールド＋コンストラクタ、または Lombok の `@RequiredArgsConstructor`）。`@Autowired` フィールドインジェクションは使わない。

## 3. DTO

- リクエストとレスポンスは別の型にする（`CreateUserRequest`、`UpdateUserRequest`、`UserResponse`）。作成時と更新時で必須項目が違うことが多く、1つの型にするとバリデーションが破綻するため。
- DTO は Java の `record` で書く。不変で、equals/toString も自動で付くため。
- **JPA の Entity を API の入出力に直接使わない。** 内部構造が外部仕様に漏れ、遅延ロードの例外や循環参照の原因にもなる。
- Entity ↔ DTO の変換は、レスポンス側に `static UserResponse from(User user)` を置くなど、変換の場所を1か所に決める。

## 4. バリデーション

- `spring-boot-starter-validation` を使い、リクエスト record のフィールドに `jakarta.validation` のアノテーション（`@NotBlank`、`@Size`、`@Email`、`@Positive` など）を付ける。
- Controller の引数に `@Valid` を付ける。パス・クエリパラメータを検証する場合はクラスに `@Validated` を付ける。
- 単項目チェックはアノテーションで、DB を見ないと分からないチェック（メールアドレスの重複など）は Service で行い、該当する業務例外を投げる。
- メッセージは日本語で、利用者が直し方を理解できる文にする（例: 「メールアドレスの形式が正しくありません」）。

## 5. エラーレスポンスの形式（ProblemDetail）

すべてのエラーは RFC 9457 の Problem Details 形式（`Content-Type: application/problem+json`）で返す。Spring Boot 3 は `ProblemDetail` クラスを標準で持っているので、独自のエラー型を作らずこれを使う。

```json
{
  "type": "about:blank",
  "title": "Validation Failed",
  "status": 400,
  "detail": "入力内容に誤りがあります",
  "instance": "/api/v1/users",
  "code": "VALIDATION_ERROR",
  "timestamp": "2026-10-26T10:15:30+09:00",
  "errors": [
    { "field": "email", "message": "メールアドレスの形式が正しくありません" }
  ]
}
```

- 標準項目（`type`、`title`、`status`、`detail`、`instance`）に加えて、拡張プロパティとして必ず `code`（機械判定用のエラーコード）と `timestamp` を付ける。バリデーションエラーのときだけ `errors` 配列を付ける。
- `code` は `ErrorCode` enum で一元管理する。クライアントは文言ではなく `code` で分岐するので、一度公開した `code` の値は変えない。
- `application.properties` に `spring.mvc.problemdetails.enabled=true` を設定する（フレームワーク内部の例外も ProblemDetail 形式に揃うため）。

## 6. 例外の設計

- 業務例外は共通の基底クラス `BusinessException`（`RuntimeException` を継承し、`ErrorCode` を持つ）から派生させる。
  - `ResourceNotFoundException` → 404
  - `ConflictException`（重複・状態不整合）→ 409
  - `BusinessRuleViolationException`（業務ルール違反）→ 422
- HTTP ステータスは `ErrorCode` enum 側に持たせる。こうすると新しいエラーを追加するとき enum に1行足すだけで済み、ハンドラを変更しなくてよい。
- Service 層は HTTP を意識しない。`ResponseStatusException` を Service で投げない（HTTP の概念が業務層に漏れるため）。

### ステータスコードの選び方

| 状況 | ステータス | ErrorCode 例 |
|---|---|---|
| 入力形式・バリデーション違反、JSON が壊れている | 400 | `VALIDATION_ERROR`、`MALFORMED_REQUEST` |
| 未認証 | 401 | `UNAUTHORIZED` |
| 権限なし | 403 | `FORBIDDEN` |
| リソースが存在しない | 404 | `RESOURCE_NOT_FOUND` |
| 一意制約違反・楽観ロック競合 | 409 | `DUPLICATE_RESOURCE`、`OPTIMISTIC_LOCK_CONFLICT` |
| 形式は正しいが業務ルール上処理できない | 422 | `BUSINESS_RULE_VIOLATION` |
| 想定外の例外 | 500 | `INTERNAL_ERROR` |

## 7. GlobalExceptionHandler

- `@RestControllerAdvice` を付け、`ResponseEntityExceptionHandler` を継承した `GlobalExceptionHandler` を1つだけ作る。継承することで、Spring MVC 標準の例外（JSON パース失敗、メソッド不一致など）も ProblemDetail 形式になる。
- 扱う例外:
  - `MethodArgumentNotValidException` → `handleMethodArgumentNotValid` をオーバーライドし、`errors` 配列を組み立てる
  - `HttpMessageNotReadableException` → `handleHttpMessageNotReadable` をオーバーライドし、`MALFORMED_REQUEST` を返す
  - `ConstraintViolationException`（パス・クエリの検証）→ 400
  - `BusinessException` → `ErrorCode` のステータスで返す
  - `ObjectOptimisticLockingFailureException` → 409
  - `Exception`（最後の砦）→ 500
- **500 のときはクライアントに内部情報を返さない。** スタックトレース、SQL、クラス名を `detail` に入れない。`detail` は「サーバー内部でエラーが発生しました」などの固定文言にする。
- ログ方針: 4xx は `WARN`（スタックトレースなし、`code` とメッセージのみ）、5xx は `ERROR`（スタックトレースあり）。4xx をすべて ERROR にすると監視アラートがノイズで埋まるため。

## 8. テスト

API のテスト方針は `testing.md` を参照。
