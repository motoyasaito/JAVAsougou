---
name: spring-boot-conventions
description: このプロジェクト（配送管理アプリ。Spring Boot 4 / Java 17以上 / Maven / MyBatis / PostgreSQL）のチーム開発規約。パッケージ構成・レイヤー設計・MyBatis、REST API・例外処理、テストの3分野をまとめたもの。このプロジェクトの Java コードを新規に書く・修正する・レビューする・リファクタリングするときは、明示的に頼まれていなくても必ずこのスキルを使うこと。「APIを作って」「エンドポイント追加」「機能を追加」「Controller/Service/Mapper/Entityを作って」「SQLを書いて」「例外処理」「エラーハンドリング」「バリデーション」「テストを書いて」「このコードをレビューして」などの依頼、および pom.xml・application.properties・schema.sql の変更が対象。
---

# 開発規約（配送管理アプリ）

このスキルは、このプロジェクトのコードを誰が書いても（Claude が書いても）同じ形になるようにするための規約です。
**既存コードの書き方を正とし、それを言葉にしたもの**です。規約は3分野に分かれていて、詳細はそれぞれの参照ファイルにあります。**作業に関係する参照ファイルを、コードを書く前に読んでください。** 全部を毎回読む必要はありません。

前提: Spring Boot 4 / Java 17以上 / Maven / MyBatis（アノテーション方式）/ PostgreSQL / `jakarta.*` パッケージ（`javax.*` は使わない）。JPA は使っていない。

## どの参照ファイルを読むか

| 作業内容 | 読むファイル |
|---|---|
| 新しい機能（Entity〜Controller一式）を作る | `references/architecture.md` → `references/rest-api.md` → `references/rest-api-templates.md` → `references/testing.md` |
| エンドポイントを追加・変更する | `references/rest-api.md`（必要なら `rest-api-templates.md`） |
| SQL・Mapper を書く・直す、テーブルを変える | `references/architecture.md` の 3〜4 |
| 例外処理・エラーレスポンスを作る・直す | `references/rest-api.md` の 5〜6 と `references/rest-api-templates.md` の 5 |
| テストを書く・直す | `references/testing.md` |
| コードレビュー | 対象に応じて上記すべて。指摘は「どの規約のどの項目に反するか」を添える |

## 全分野に共通する原則

細かいルールは参照ファイルにあるが、迷ったときはこの原則に立ち返る。

1. **レイヤー単位でまとめる。** `controller/` `service/` `mapper/` `dto/` `entity/` `exception/` にそれぞれ置く。
2. **依存は Controller → Service → Mapper の一方向。** Controller は HTTP の変換係、Service は業務ロジック、Mapper は SQL だけを担当する。
3. **Entity を外に出さない。** API の入出力は `record` の DTO。Entity → DTO の変換は DTO の `static from(...)` で行う。
4. **SQL の値は `#{}` で渡す。** `${}` は使わない。メソッド名と SQL の中身を一致させる。
5. **例外は投げっぱなしにして一か所で受ける。** Controller・Service に try-catch を書かず、`GlobalExceptionHandler` が `ErrorResponse` に変換する。
6. **コンストラクタインジェクションのみ。** フィールドは `private final`。

## 規約と既存コードが食い違う場合

既存コードがこの規約と違う書き方をしている場合は、勝手に全体を書き換えない。
新しく書く部分は規約に従い、既存部分との違いがあることをユーザーに伝えて、合わせるか直すかを確認する。
大規模な書き換えは差分が大きくなり、レビューも動作確認も難しくなるため。

規約に書かれていないことは、まず既存コードの書き方に合わせる。既存コードにも例がなければ、ユーザーに確認する。

## 作業後のセルフチェック

コードを出す前に確認する。

- [ ] 正しいレイヤーのパッケージに置いたか
- [ ] Controller に業務ロジック・try-catch・Mapper 呼び出しがないか
- [ ] Entity を返していないか。DTO は record で、`from` で変換しているか
- [ ] Mapper のメソッド名と SQL の中身（WHERE 条件・引数）が一致しているか
- [ ] SQL で `${}` や `SELECT *` を使っていないか。カラム名は `schema.sql` と一致しているか
- [ ] ページングに `ORDER BY` があり、一覧と件数（count）の条件が揃っているか
- [ ] 新しい例外・エラーケースは `GlobalExceptionHandler` で `ErrorResponse` の形にしているか
- [ ] 500 のレスポンスに内部情報が漏れていないか
- [ ] 更新系の Service メソッドに `@Transactional` を付けたか
- [ ] フィールドインジェクションを使っていないか
- [ ] `javax.*` ではなく `jakarta.*` を import しているか
- [ ] ログに個人情報（氏名・住所・電話番号・メールアドレス）を出していないか
