# パッケージ構成・レイヤー設計 規約

## 目次
1. パッケージ構成
2. レイヤーの責務と依存の向き
3. Entity と Repository
4. トランザクション
5. DI とクラス設計
6. 設定値
7. ログ
8. 命名規則

---

## 1. パッケージ構成

**機能（ドメイン）単位でパッケージを切り、その中にレイヤーを置く**（package by feature）。
層ごと（`controller/`、`service/` …）に切ると、1つの機能を直すために複数の遠いパッケージを行き来することになり、機能が増えるほど見通しが悪くなるため。

```
src/main/java/com/example/app/
├── Application.java
├── common/                     全機能で共有するもの（ここに業務ロジックを置かない）
│   ├── config/                 @Configuration、@ConfigurationProperties
│   └── exception/              ErrorCode、BusinessException、GlobalExceptionHandler
├── user/
│   ├── UserController.java
│   ├── UserService.java
│   ├── UserRepository.java
│   ├── User.java               Entity
│   └── dto/
│       ├── CreateUserRequest.java
│       ├── UpdateUserRequest.java
│       └── UserResponse.java
└── order/
    └── ...（user と同じ形）
```

- 機能パッケージの中でさらに `controller/` `service/` のようなサブパッケージは作らない。ファイル数が10を超えて見通しが悪くなったら、`dto/` 以外に `domain/` などを切ることを検討する。
- `common/` は「どの機能からも使われる技術的な部品」だけを置く場所。特定の機能の都合が入ったら、その機能のパッケージに戻す。
- `util/` パッケージの乱立を避ける。staticメソッドの寄せ集めは、置き場所がない処理の逃げ場になりやすい。

## 2. レイヤーの責務と依存の向き

依存は **Controller → Service → Repository** の一方向のみ。逆向き・飛び越しは禁止。

| レイヤー | やること | やらないこと |
|---|---|---|
| Controller | HTTP の受け取り、`@Valid`、Service 呼び出し、`ResponseEntity` 組み立て | 業務ロジック、try-catch、Repository の直接呼び出し、Entity の返却 |
| Service | 業務ロジック、トランザクション境界、Entity ↔ DTO 変換の呼び出し、業務例外の送出 | HTTP の概念（`HttpServletRequest`、`ResponseStatusException`、`ResponseEntity`） |
| Repository | 永続化（Spring Data JPA） | 業務判断 |

- 他の機能のデータが必要な場合は、**相手の Service を呼ぶ**。相手の Repository を直接呼ばない（相手機能のルールを迂回してしまうため）。
- Service 同士が循環参照しそうになったら、設計を見直す合図。共通部分を別の Service に切り出すか、どちらが主かを決める。
- Service がインターフェース＋Impl の2点セットになっている必要はない。実装が1つしかないならクラスだけでよい（モックは Mockito でクラスに対して作れる）。

## 3. Entity と Repository

- Entity は `jakarta.persistence` のアノテーションを使う。
- ID は `@GeneratedValue(strategy = GenerationType.IDENTITY)` の `Long` を基本とする。
- 関連は **`FetchType.LAZY` を基本**にする（`@ManyToOne` はデフォルトが EAGER なので明示的に LAZY を付ける）。必要なところだけ `@EntityGraph` か JPQL の `join fetch` で取得し、N+1 を防ぐ。
- 同時更新が起きうる Entity には `@Version` を付けて楽観ロックにする（競合時は 409 を返す。`rest-api.md` 参照）。
- 作成日時・更新日時は `@CreationTimestamp` / `@UpdateTimestamp`、または JPA Auditing（`@CreatedDate` / `@LastModifiedDate`）で自動設定する。
- Entity の setter は必要なものだけ。状態変更は意味のあるメソッド名にする（`order.cancel()` のように）。これにより「どこで状態が変わるか」を検索しやすくなる。
- Entity に Lombok の `@Data` を付けない（`equals`/`hashCode`/`toString` が関連を辿って遅延ロード例外や無限ループを起こすため）。
- Repository は `JpaRepository<Entity, Long>` を継承。クエリはまずメソッド名派生（`findByEmail`）、複雑なら `@Query`（JPQL）。ネイティブSQLは最後の手段。
- `Open Session in View` は無効にする（`spring.jpa.open-in-view=false`）。有効だと Controller でうっかり遅延ロードが走り、どこでSQLが出ているか追えなくなるため。

## 4. トランザクション

- `@Transactional` は **Service のクラスに `@Transactional(readOnly = true)`**、更新系メソッドにだけ `@Transactional` を付ける。
- Controller と Repository には付けない。
- `org.springframework.transaction.annotation.Transactional` を使う（`jakarta.transaction.Transactional` と取り違えない）。
- 同じクラス内のメソッド呼び出しには `@Transactional` が効かない（プロキシを経由しないため）。トランザクションを分けたい場合は別の Bean に切り出す。
- トランザクション内で外部APIの呼び出しやメール送信などの遅い処理をしない（DBコネクションを長時間占有するため）。

## 5. DI とクラス設計

- **コンストラクタインジェクションのみ**。フィールドは `private final`。コンストラクタが1つならば `@Autowired` は不要。
- Lombok を使っている場合は `@RequiredArgsConstructor` でよい。使っていない場合は手書きする。
- `@Component` 系の Bean に可変の状態（フィールドの書き換え）を持たせない。Bean はシングルトンで、複数リクエストから同時に使われるため。
- Java 17 の機能を積極的に使う: DTO は `record`、テキストブロック、`switch` 式、パターンマッチング `instanceof`。

## 6. 設定値

- 値は `application.yml`（または `.properties`）に置き、環境差分はプロファイル（`application-local.yml`、`application-prod.yml`）で分ける。
- まとまった設定は `@Value` を散らばらせず、`@ConfigurationProperties` を付けた `record` で受ける。型安全でテストもしやすいため。

```java
@ConfigurationProperties(prefix = "app.mail")
public record MailProperties(String from, Duration timeout) {}
```

（`@EnableConfigurationProperties(MailProperties.class)` または `@ConfigurationPropertiesScan` で有効化する）

- パスワード・APIキーなどの秘密情報をリポジトリにコミットしない。環境変数から `${DB_PASSWORD}` のように受ける。

## 7. ログ

- SLF4J を使う（`LoggerFactory.getLogger(Xxx.class)`、Lombok があれば `@Slf4j`）。`System.out.println` は使わない。
- 文字列連結ではなくプレースホルダを使う: `log.info("user created: id={}", id)`。
- レベルの目安: `ERROR` = 対応が必要な異常（5xx）、`WARN` = 想定内だが注意すべき事象（4xx、リトライ）、`INFO` = 業務上の主要イベント、`DEBUG` = 開発時の詳細。
- 個人情報（メールアドレス、氏名、トークン、パスワード）をログに出さない。出す必要があるときは ID で代替する。

## 8. 命名規則

| 種類 | 規則 | 例 |
|---|---|---|
| Controller | `{機能}Controller` | `UserController` |
| Service | `{機能}Service` | `UserService` |
| Repository | `{Entity}Repository` | `UserRepository` |
| リクエストDTO | `{動詞}{機能}Request` | `CreateUserRequest` |
| レスポンスDTO | `{機能}Response` | `UserResponse` |
| 例外 | `{内容}Exception` | `ResourceNotFoundException` |
| 設定 | `{対象}Config` / `{対象}Properties` | `SecurityConfig` / `MailProperties` |
| テスト | `{対象クラス}Test`（結合テストは `{対象}IT`） | `UserServiceTest` / `UserApiIT` |

- メソッド名: 取得は `find`（なければ例外を投げる場合も `findById` で統一し、`Optional` を返すのは Repository のみ）、作成 `create`、更新 `update`、削除 `delete`。
- 定数は `UPPER_SNAKE_CASE`、パッケージ名はすべて小文字。
