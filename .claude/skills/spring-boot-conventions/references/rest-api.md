# REST API・例外処理 規約

コード雛形は `rest-api-templates.md` にある。新しい Controller・例外・ハンドラを書くときは併せて読むこと。

## 目次
1. URL と HTTP メソッド
2. Controller の責務
3. DTO
4. バリデーション
5. エラーレスポンスの形式（ErrorResponse）
6. 例外と GlobalExceptionHandler
7. テスト

---

## 1. URL と HTTP メソッド

- ベースパスは `/api`、その下に**利用者**（`admin` / `driver`）、その下にリソースを置く。
  - 例: `/api/admin/deliveries`、`/api/driver/deliveries`
  - URL にバージョン（`/v1`）は入れない。
- リソース名は複数形の名詞・ケバブケース（`/api/admin/re-deliveries`）。動詞は URL に入れず、HTTP メソッドで表す。
- 対応表:

| 操作 | メソッド | パス | 成功時ステータス |
|---|---|---|---|
| 一覧取得 | GET | `/api/admin/deliveries` | 200 |
| 1件取得 | GET | `/api/admin/deliveries/{id}` | 200 |
| 作成 | POST | `/api/admin/deliveries` | 201 |
| 全体更新 | PUT | `/api/admin/deliveries/{id}` | 200 |
| 部分更新（ステータス変更など） | PATCH | `/api/driver/deliveries/{id}/status` | 200 |
| 削除 | DELETE | `/api/admin/deliveries/{id}` | 204（ボディなし） |

- 一覧は `page`（0始まり）と `size` のクエリパラメータでページングし、`PageResponse<T>` で返す。デフォルトは `page=0`、`size=10`、size の上限は 100。
- JSON のプロパティ名は camelCase（Jackson のデフォルトのまま）。日付は `LocalDate`（`2026-10-26` 形式）。

## 2. Controller の責務

Controller は「HTTP の変換係」に徹する。業務ロジックが Controller に混ざると、テストしにくく、例外処理の流れも読みにくくなるため。

- `@RestController` + `@RequestMapping("/api/{利用者}/{リソース}")` を付ける。
- 入力の受け取り（`@Valid @RequestBody`、`@PathVariable`、`@RequestParam`）→ Service 呼び出し → 返す、だけを書く。
- 200 で返すときは DTO をそのまま戻り値にする（`ResponseEntity` で包まない）。201・204 など 200 以外を返すときだけ `ResponseEntity` を使う。
- **Controller に try-catch を書かない。** 例外は Service から投げっぱなしにし、`GlobalExceptionHandler` に任せる。
- コンストラクタインジェクション。`@Autowired` フィールドインジェクションは使わない。

## 3. DTO

- DTO は `dto/` パッケージに Java の `record` で書く。
- リクエストとレスポンスは別の型にする（`UpdateDeliveryStatusRequest`、`DeliveryResponse`）。
- **Entity を API の入出力に直接使わない。** テーブルの構造がそのまま外に出てしまい、`password_hash` のような項目まで漏れる危険がある。
- Entity → DTO の変換は、レスポンス DTO に `public static XxxResponse from(Entity e)` を置いて行う。
- 画面で使わない項目は DTO に入れない（`DeliveryResponse` が `createdAt` / `updatedAt` を返していないのと同じ考え方）。
- 一覧の共通の形は `PageResponse<T>` を使う。新しく作らない。

## 4. バリデーション

- `spring-boot-starter-validation` を使い、`jakarta.validation` のアノテーション（`@NotBlank`、`@Size`、`@Pattern`、`@Min`、`@Max` など）を付ける。
- **リクエストボディ**: record のフィールドにアノテーションを付け、Controller の引数に `@Valid @RequestBody` を付ける。違反すると `MethodArgumentNotValidException` になる。
- **パス・クエリパラメータ**: 引数に直接 `@Min` などを付ける（`AdminDeliveryController` のとおり）。違反すると `HandlerMethodValidationException` になる。
- 単項目チェックはアノテーションで、DB を見ないと分からないチェック（伝票番号の重複など）は Service で行い、例外を投げる。
- `schema.sql` の制約とアプリ側のチェックを揃える（例: 電話番号は `@Pattern(regexp = "^[0-9]{10,11}$")`、伝票番号は `^[0-9]{12}$`）。DB の制約違反で 500 になる前に、400 で返すため。
- メッセージは日本語で、利用者が直し方を分かる文にする（例: `@NotBlank(message = "お届け先の氏名を入力してください")`）。

## 5. エラーレスポンスの形式（ErrorResponse）

すべてのエラーは `dto/ErrorResponse` の形で返す。ProblemDetail は使わない。

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "入力内容に誤りがあります",
  "details": ["size:100 以下の値にしてください"]
}
```

| 項目 | 内容 |
|---|---|
| `status` | HTTP ステータスコード |
| `error` | ステータスの名前（`HttpStatus.getReasonPhrase()`） |
| `message` | 画面に出す日本語のメッセージ |
| `details` | 入力チェックで引っかかった項目の一覧（`項目名:メッセージ`）。なければ空配列 |

- `ErrorResponse` は `GlobalExceptionHandler` の `build` メソッドでだけ作る。Controller や Service で直接作らない。

## 6. 例外と GlobalExceptionHandler

`exception/GlobalExceptionHandler`（`@RestControllerAdvice`）が全 Controller の例外を受け取り、`ErrorResponse` に変換する。

### 現在扱っている例外

| 例外 | ステータス | message |
|---|---|---|
| `MethodArgumentNotValidException`（ボディの入力チェック） | 400 | 入力内容に誤りがあります（`details` に項目一覧） |
| `HttpMessageNotReadableException`（JSON が壊れている） | 400 | リクエストの形式が正しくありません |
| `NoResourceFoundException`（存在しない URL） | 404 | 指定したURLは存在しません |
| `HttpRequestMethodNotSupportedException` | 405 | このURLでは{メソッド}は使えません |
| `Exception`（上記以外すべて） | 500 | サーバーでエラーが発生しました |

### 追加するときのルール

- `@ExceptionHandler` で受けていない例外は、**すべて最後の `Exception` ハンドラに落ちて 500 になる。** 新しい種類のエラーを起こすコードを書いたら、対応するハンドラがあるか必ず確認する。
  - 例: クエリパラメータの `@Min` 違反（`HandlerMethodValidationException`）、`?page=abc` のような型違い（`MethodArgumentTypeMismatchException`）、必須パラメータの欠落（`MissingServletRequestParameterException`）は、400 で返すハンドラが必要。
- 業務上のエラーは自作の例外クラスを `exception/` に作り、`RuntimeException` を継承する。

| 状況 | ステータス | 例外クラス例 |
|---|---|---|
| 指定した ID のデータが存在しない | 404 | `ResourceNotFoundException` |
| 重複・状態の不整合（配達済みの荷物を再度配達済みにする など） | 409 | `ConflictException` |
| 権限がない（他のドライバーの荷物を操作する など） | 403 | `ForbiddenException` |

- 新しい例外クラスを作ったら、`GlobalExceptionHandler` にハンドラを追加し、`build` で `ErrorResponse` を返す。
- Service で `ResponseStatusException` を投げない（HTTP の概念が業務層に漏れるため）。
- データが見つからないときに `null` を返さない。Service で `ResourceNotFoundException` を投げる。
- **500 のときはクライアントに内部情報を返さない。** スタックトレース・SQL・クラス名を `message` に入れず、固定文言にする。
- ログ方針: 4xx はログに出さないか `WARN`（スタックトレースなし）、500 は `ERROR`（スタックトレースあり。`log.error("...", e)`）。

## 7. テスト

API のテスト方針は `testing.md` を参照。
