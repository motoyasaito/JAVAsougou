# テスト 規約

## 目次
1. 使うライブラリ
2. テストの種類と使い分け
3. 書き方の共通ルール
4. Service の単体テスト
5. Controller のテスト（@WebMvcTest）
6. Repository のテスト（@DataJpaTest + Testcontainers）
7. 結合テスト（@SpringBootTest）
8. Maven での実行

---

## 1. 使うライブラリ

`spring-boot-starter-test` に含まれるものを使う: JUnit 5、AssertJ、Mockito、MockMvc。
アサーションは **AssertJ（`assertThat`）に統一**する。JUnit の `assertEquals` と混在させない（読み方が揃わず、失敗メッセージも AssertJ の方が分かりやすいため）。

DB を使うテストは Testcontainers で本番と同じ種類のDBを起動する。H2 は使わない（SQL方言や制約の挙動が本番と違い、「テストは通るのに本番で落ちる」原因になるため）。

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-testcontainers</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>postgresql</artifactId> <!-- 使っているDBに合わせる -->
    <scope>test</scope>
</dependency>
```

## 2. テストの種類と使い分け

速いテストを多く、遅いテストを少なく。

| 対象 | 方法 | Spring 起動 | 主に確認すること |
|---|---|---|---|
| Service | 素の JUnit + Mockito | しない | 業務ロジック、例外を投げる条件 |
| Controller | `@WebMvcTest` | Web層のみ | ステータス、JSON の形、バリデーション、エラー形式 |
| Repository | `@DataJpaTest` + Testcontainers | JPA層のみ | 自作クエリ、制約 |
| 全体 | `@SpringBootTest` + Testcontainers | 全部 | 主要な業務フローが端から端まで動くか |

- Service の単体テストで Spring を起動しない。`@SpringBootTest` を何にでも使うと、テストが遅くなって誰も実行しなくなる。
- Repository はメソッド名派生クエリだけなら書かなくてよい。`@Query` を書いたものは必ずテストする。

## 3. 書き方の共通ルール

- テストメソッド名は **日本語で「条件と期待結果」** を書く: `存在しないIDなら404を返す()`。英語にする場合は `shouldReturn404WhenUserNotFound` 形式。プロジェクト内でどちらかに統一する。
- 本体は **Given / When / Then** の3ブロックに分け、空行かコメントで区切る。
- 1テストで確認する振る舞いは1つ。
- 正常系だけでなく、境界値（0、空文字、最大長、最大長+1）と異常系を書く。
- テスト間で状態を共有しない。実行順序に依存させない。
- 現在時刻に依存する処理は `Clock` を Bean として注入し、テストでは `Clock.fixed(...)` を渡す。

## 4. Service の単体テスト

```java
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock UserRepository userRepository;
    @InjectMocks UserService userService;

    @Test
    void 登録済みのメールアドレスならConflictExceptionを投げる() {
        // Given
        var request = new CreateUserRequest("山田太郎", "taro@example.com");
        when(userRepository.existsByEmail("taro@example.com")).thenReturn(true);

        // When / Then
        assertThatThrownBy(() -> userService.create(request))
                .isInstanceOf(ConflictException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.DUPLICATE_RESOURCE);
        verify(userRepository, never()).save(any());
    }
}
```

## 5. Controller のテスト（@WebMvcTest）

- `@WebMvcTest(XxxController.class)` で対象を絞る。`@RestControllerAdvice` は自動で読み込まれるので、エラー形式もここで検証できる。
- Service は `@MockitoBean`（Spring Boot 3.4以上。3.3以前は `@MockBean`）。
- 各エンドポイントで最低限: 正常系 / バリデーションエラー（400・`errors` の中身）/ 存在しない（404）。
- エラー時は `status`、`code`、`Content-Type: application/problem+json` を検証し、`detail` の文言は検証しない（変わりやすいため）。
- コード例は `rest-api-templates.md` の「8. Controller テスト」を参照。

## 6. Repository のテスト（@DataJpaTest + Testcontainers）

```java
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class UserRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired UserRepository userRepository;

    @Test
    void メールアドレスで存在確認できる() {
        userRepository.save(new User("山田太郎", "taro@example.com"));

        assertThat(userRepository.existsByEmail("taro@example.com")).isTrue();
        assertThat(userRepository.existsByEmail("none@example.com")).isFalse();
    }
}
```

- `@ServiceConnection` を使えば接続情報の設定は不要。
- コンテナ定義が複数のテストクラスで重複する場合は、`@TestConfiguration` に `@Bean @ServiceConnection` でまとめて `@Import` する。
- `@DataJpaTest` は各テスト後にロールバックされるので、テストデータの後片付けは不要。

## 7. 結合テスト（@SpringBootTest）

- `@SpringBootTest(webEnvironment = RANDOM_PORT)` + Testcontainers で、主要な業務フロー（例: ユーザー登録 → 取得 → 削除）だけを確認する。全パターンをここで網羅しない。
- クラス名は `{対象}IT` とし、Maven の Failsafe プラグインで `verify` フェーズに実行する（単体テストと分けて、普段の `mvn test` を速く保つため）。
- `@SpringBootTest` のテストは `@Transactional` でロールバックされないことがある（別スレッドのHTTPリクエストはテストのトランザクション外で動くため）。`@AfterEach` で明示的にデータを消すか、テストごとに一意なデータを使う。

## 8. Maven での実行

```bash
mvn test        # 単体テスト・スライステスト（*Test）
mvn verify      # 上記 + 結合テスト（*IT, Failsafe）
```

Failsafe の設定（`pom.xml`）:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-failsafe-plugin</artifactId>
    <executions>
        <execution>
            <goals>
                <goal>integration-test</goal>
                <goal>verify</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

（バージョンは spring-boot-starter-parent が管理するので指定不要）
