# パッケージ構成・レイヤー設計・MyBatis 規約

## 目次
1. パッケージ構成
2. レイヤーの責務と依存の向き
3. Mapper と SQL
4. Entity とテーブル
5. トランザクション
6. DI とクラス設計
7. 設定値
8. ログ
9. 命名規則
10. コメント

---

## 1. パッケージ構成

**レイヤー単位でパッケージを切る**（package by layer）。

```
src/main/java/com/example/demo/
├── DemoApplication.java
├── controller/     @RestController（AdminDeliveryController など）
├── service/        @Service（DeliveryService など）
├── mapper/         MyBatis の @Mapper インターフェース（DeliveryMapper など）
├── entity/         テーブル1行に対応するクラス（Delivery、User など）
├── dto/            API の入出力の record（DeliveryResponse、PageResponse、ErrorResponse など）
└── exception/      GlobalExceptionHandler、自作の例外クラス

src/main/resources/
├── application.properties
├── schema.sql      テーブル定義（起動のたびに作り直す）
└── data.sql        初期データ
```

- 上記以外のパッケージを増やすときは、ユーザーに確認する。設定クラスが必要になったら `config/` を作る。
- `util/` パッケージは作らない。staticメソッドの寄せ集めは、置き場所がない処理の逃げ場になりやすい。

## 2. レイヤーの責務と依存の向き

依存は **Controller → Service → Mapper** の一方向のみ。逆向き・飛び越しは禁止。

| レイヤー | やること | やらないこと |
|---|---|---|
| Controller | HTTP の受け取り、入力チェック（`@Valid`、`@Min` など）、Service 呼び出し、DTO を返す | 業務ロジック、try-catch、Mapper の直接呼び出し、Entity の返却 |
| Service | 業務ロジック、Entity → DTO 変換の呼び出し、ページング計算、業務例外の送出、トランザクション境界 | HTTP の概念（`HttpServletRequest`、`ResponseEntity`、`ResponseStatusException`） |
| Mapper | SQL の実行だけ | 業務判断 |

- 管理者用とドライバー用で画面が違っても、同じテーブルを扱うなら Service・Mapper は共通にしてよい（例: `AdminDeliveryController` と `DriverDeliveryController` が両方 `DeliveryService` を使う）。Controller は利用者ごとに分ける。
- 他のテーブルのデータが必要なとき、単純な参照なら SQL の JOIN で取る。業務ルールが絡むなら、そのテーブルを担当する Service を呼ぶ。
- Service はインターフェース＋Impl の2点セットにしない。実装が1つならクラスだけでよい。

## 3. Mapper と SQL

MyBatis は **アノテーション方式**（`@Select` / `@Insert` / `@Update` / `@Delete`）で書く。XML マッパーは使わない。

```java
@Mapper
public interface DeliveryMapper {

  // 担当ドライバーの配送物をID順に、指定した件数だけ取得する
  @Select("""
      SELECT id, tracking_number, name, address, call_number, email, status, item_type,
             driver_id, delivery_date, delivery_time, created_at, updated_at
      FROM deliveries
      WHERE driver_id = #{driverId}
      ORDER BY id
      LIMIT #{limit} OFFSET #{offset}
      """)
  List<Delivery> findByDriverId(
      @Param("driverId") long driverId, @Param("limit") int limit, @Param("offset") int offset);

  @Select("SELECT COUNT(*) FROM deliveries WHERE driver_id = #{driverId}")
  long countByDriverId(@Param("driverId") long driverId);
}
```

- **メソッド名と SQL の中身を一致させる。** `findByDriverId` なら `WHERE driver_id = #{driverId}` があり、引数に `driverId` がある。名前と中身がずれると、呼び出す側が誤解したまま使ってしまう。
- **値は必ず `#{}` で渡す。`${}` は使わない。** `${}` は文字列をそのまま SQL に埋め込むため、SQLインジェクションの原因になる。並び替え項目を切り替えたいなどで `${}` が必要に見えたら、ユーザーに相談する。
- 引数が2つ以上のときは、すべてに `@Param` を付け、SQL のプレースホルダと同じ名前にする。
- 複数行の SQL はテキストブロック（`"""`）で書く。
- `SELECT *` は使わず、カラムを列挙する。テーブルにカラムが増えたとき、意図しないデータまで取得してしまうため。
- カラム名は `schema.sql` と一致させる。snake_case のカラムは `mybatis.configuration.map-underscore-to-camel-case=true` により Entity の camelCase フィールドに自動で対応する。SQL 側で `AS` を付ける必要はない。
- **ページング**
  - `LIMIT #{limit} OFFSET #{offset}` には必ず `ORDER BY` を付ける。付けないと、ページをまたいで同じ行が出たり、出ない行ができたりする。
  - 一覧取得と件数取得（`count`）は対にし、**WHERE 条件を揃える**。揃っていないとページ数がずれる。
  - offset の計算（`page * size`）は Service で行う。
- 更新系
  - `UPDATE` では `updated_at = now()` を必ず設定する（`schema.sql` のコメントの方針）。
  - 更新・削除は戻り値を `int`（更新件数）にし、0件なら Service で「見つからない」として扱う。
- メソッド名: 1件取得 `findById`、一覧 `findAll` / `findByXxx`、件数 `count` / `countByXxx`、追加 `insert`、更新 `update` / `updateXxx`、削除 `delete`。
- 1件取得は戻り値を `Optional<Entity>` にしてよい。存在しない場合の判断は Service で行う。

## 4. Entity とテーブル

- Entity はテーブル1行に対応する普通のクラス（getter / setter）。JPA のアノテーションは付けない。
- Lombok は使っていない。getter / setter は手書きする（IDE の自動生成でよい）。
- フィールドの型
  - `BIGSERIAL` / `BIGINT` → `Long`（NULL がありうるため、`driverId` などの外部キーもラッパー型）
  - `VARCHAR` → `String`
  - `DATE` → `LocalDate`、`TIMESTAMP` → `LocalDateTime`
- `status` や `item_type` のように値が決まっているカラムは、`schema.sql` の `CHECK` 制約が正。アプリ側で値を増やすときは `schema.sql` も一緒に変える。
- テーブルを変えたら、`schema.sql`・`data.sql`・Entity・関係する Mapper の SQL をまとめて直す。
- `users.password_hash` のような機密項目は、Entity には持ってよいが、DTO には絶対に入れない。

## 5. トランザクション

- 参照だけの Service メソッドには付けなくてよい（現状の `DeliveryService.findAll` のとおり）。
- **更新系（INSERT / UPDATE / DELETE を呼ぶ）の Service メソッドには `@Transactional` を付ける。** 複数の SQL を実行する途中で失敗したとき、全部取り消すため。
- `org.springframework.transaction.annotation.Transactional` を使う（`jakarta.transaction.Transactional` と取り違えない）。
- Controller と Mapper には付けない。
- 同じクラス内のメソッド呼び出しには `@Transactional` が効かない（プロキシを経由しないため）。

## 6. DI とクラス設計

- **コンストラクタインジェクションのみ**。フィールドは `private final`。コンストラクタが1つなら `@Autowired` は不要。

```java
private final DeliveryService deliveryService;

public AdminDeliveryController(DeliveryService deliveryService) {
  this.deliveryService = deliveryService;
}
```

- `@Service` などの Bean に可変の状態（フィールドの書き換え）を持たせない。Bean は1つのインスタンスを複数リクエストで同時に使うため。
- Java 17 の機能を使う: DTO は `record`、SQL はテキストブロック、`switch` 式。
- インデントはスペース2つ（既存コードに合わせる）。

## 7. 設定値

- 設定は `application.properties` に書く。
- DB のパスワードなどの秘密情報をリポジトリにコミットしない。環境変数から `${DB_PASSWORD}` のように受ける。
- `spring.sql.init.mode=always` により、起動のたびに `schema.sql` でテーブルを作り直し、`data.sql` を流す。開発用の設定なので、本番相当の環境では変える必要がある。

## 8. ログ

- SLF4J を使う（`private static final Logger log = LoggerFactory.getLogger(Xxx.class);`）。`System.out.println` は使わない。
- 文字列連結ではなくプレースホルダを使う: `log.info("配送状況を更新しました: id={}", id)`。
- レベルの目安: `ERROR` = 想定外の異常（500）、`WARN` = 想定内だが注意すべき事象、`INFO` = 業務上の主要イベント、`DEBUG` = 開発時の詳細。
- **個人情報をログに出さない。** 配送先の氏名・住所・電話番号・メールアドレス、パスワードは出さず、ID や伝票番号で代わりにする。

## 9. 命名規則

| 種類 | 規則 | 例 |
|---|---|---|
| Controller | `{利用者}{機能}Controller` | `AdminDeliveryController`、`DriverDeliveryController` |
| Service | `{機能}Service` | `DeliveryService` |
| Mapper | `{Entity}Mapper` | `DeliveryMapper` |
| Entity | テーブル名の単数形・PascalCase | `deliveries` → `Delivery` |
| リクエストDTO | `{動詞}{機能}Request` | `UpdateDeliveryStatusRequest` |
| レスポンスDTO | `{機能}Response` | `DeliveryResponse` |
| 例外 | `{内容}Exception` | `ResourceNotFoundException` |
| テスト | `{対象クラス}Test` | `DeliveryServiceTest` |

- メソッド名は lowerCamelCase。`findBy` の `B` のように、単語の区切りは必ず大文字にする（`findbyDriverId` ではなく `findByDriverId`）。
- 定数は `UPPER_SNAKE_CASE`、パッケージ名はすべて小文字。

## 10. コメント

- コメントは日本語で書く。
- メソッドの上に「何をするか」を1行で書く（既存の Mapper・Service のとおり）。
- 計算や条件が分かりにくい行には、具体例を添える（例: `// 例: page=2, size=10 なら21件目から（20件飛ばす）`）。
- コードを読めば分かること（`// getterです` など）は書かない。
