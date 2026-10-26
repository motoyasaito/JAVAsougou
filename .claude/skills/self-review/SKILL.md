---
name: self-review
description: このプロジェクト（Spring Boot 4 + MyBatis + PostgreSQL の配送管理アプリ）の変更差分をセルフレビューする。コミット前・PR作成前に、MyBatis の SQL、レイヤー構成、エラー処理、入力チェック、命名、テスト、Git の観点で問題を指摘する。「セルフレビューして」「コミット前に見て」「PR出す前にチェックして」「/self-review」などの依頼で使う。指摘のみ行い、コードは自動で直さない。
---

# セルフレビュー

自分が書いた変更を、コミット前・PR作成前に見直すためのスキル。
**指摘だけを行い、コードは修正しない。** 直すかどうかはユーザーが判断する。ユーザーが「直して」と頼んだときだけ修正する。

## 1. 対象を集める

次のコマンドで差分を集める。

```
git branch --show-current
git status --short
git diff main...HEAD      # ブランチでコミット済みの変更
git diff                  # 未コミットの変更
git diff --cached         # ステージ済みの変更
git log main..HEAD --oneline
```

- 差分に出てくるクラスは、ファイル全体を読む（差分だけでは文脈が分からないため）
- 呼び出し元・呼び出し先（Controller → Service → Mapper）も必要に応じて読む
- `src/main/resources/schema.sql` を読み、SQL のカラム名・テーブル名が実在するか確かめる
- 差分がない場合は、その旨を伝えて終了する

## 2. チェック観点

このプロジェクトの構成は次のとおり。これを基準に判断する。

- パッケージはレイヤー単位：`controller/` `service/` `mapper/` `dto/` `entity/` `exception/`
- 永続化は MyBatis のアノテーション（`@Select` など）
- エラーは `GlobalExceptionHandler`（`@RestControllerAdvice`）で受け、`ErrorResponse` DTO で返す

規約の詳細は `spring-boot-conventions` スキルにある。指摘の根拠として必要なら、その参照ファイルを読む。

### MyBatis / SQL

- [ ] **メソッド名と SQL の中身が一致しているか。** `findByXxx` なら `WHERE xxx = #{xxx}` があり、引数にも `xxx` があるか。名前と中身のずれは見落としやすいので重点的に見る
- [ ] `${}` を使っていないか。値は必ず `#{}` で渡す（SQLインジェクション対策）
- [ ] `@Param` の名前と SQL のプレースホルダ名が一致しているか
- [ ] SELECT するカラムが `schema.sql` に存在し、Entity のフィールドと対応しているか
- [ ] ページング（`LIMIT` / `OFFSET`）に `ORDER BY` が付いているか
- [ ] 一覧取得と件数取得（`count`）の WHERE 条件が揃っているか。揃っていないとページ数がずれる
- [ ] `SELECT *` を使っていないか

### レイヤー構成

- [ ] Controller が Mapper を直接呼んでいないか（Controller → Service → Mapper の一方向）
- [ ] Controller に業務ロジックを書いていないか
- [ ] Entity をそのまま API で返していないか。`DeliveryResponse` などの DTO に変換しているか
- [ ] 依存はコンストラクタインジェクションか。フィールドは `private final` か（`@Autowired` フィールドは使わない）

### エラー処理

- [ ] Controller・Service に try-catch で例外を握りつぶしていないか。例外は `GlobalExceptionHandler` で受ける
- [ ] 新しいエラーケースを足した場合、`GlobalExceptionHandler` に対応するハンドラがあり、`ErrorResponse` の形で返しているか
- [ ] 500 のレスポンスにスタックトレースや SQL などの内部情報を含めていないか
- [ ] データが見つからない場合に、`null` を返さず 404 として扱っているか

### 入力チェック

- [ ] リクエストボディの DTO に `@Valid` とバリデーションアノテーションが付いているか
- [ ] `page` / `size` などのクエリパラメータに範囲チェックがあるか（負の値、極端に大きい size）

### 命名・可読性

- [ ] Java の命名規則に従っているか（例：`findbyDriverId` → `findByDriverId`、クラスは PascalCase、定数は UPPER_SNAKE_CASE）
- [ ] 使っていない import・変数・メソッドが残っていないか
- [ ] `System.out.println` やデバッグ用のコードが残っていないか
- [ ] ログに個人情報（氏名・住所・電話番号・メールアドレス）を出していないか

### テスト

- [ ] 変更した Service・Controller に対応するテストがあるか
- [ ] 正常系だけでなく、バリデーションエラーや 404 などの異常系もテストしているか
- テストがない場合は「必須」ではなく「推奨」として指摘する

### Git

- [ ] コミットメッセージにチケット番号（`HWLA-xxx`）が入っているか（推奨）
- [ ] 関係のないファイル（IDE の設定ファイル、ビルド成果物、`.env` など）が混ざっていないか
- [ ] 1つのブランチに無関係な変更が混ざっていないか

## 3. 出力形式

指摘は重要度順に並べる。各指摘には次を書く。

```
### [必須] メソッド名と SQL の中身が一致していない
- 場所: src/main/java/com/example/demo/mapper/DeliveryMapper.java:26
- 問題: findbyDriverId という名前だが WHERE driver_id の条件がなく、全件をページングして返している
- 直し方: 引数に driverId を追加し、SQL に `WHERE driver_id = #{driverId}` を足す
```

- 重要度は2段階
  - **必須**：バグ・セキュリティ問題・規約違反。直さないとマージしないほうがよいもの
  - **推奨**：命名・可読性・テスト不足など。直したほうがよいもの
- 場所は `ファイル:行` で書く
- 問題がなかった観点は、最後に「問題なし：MyBatis / エラー処理 …」のように一行でまとめる
- 指摘が1つもなければ、そう伝える。無理に指摘を作らない
- 最後に「直したい指摘があれば言ってください」と添える。自分からは修正しない
