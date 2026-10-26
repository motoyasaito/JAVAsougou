---
name: spring-boot-conventions
description: Spring Boot 3.x（Java 17以上・Maven）プロジェクトのチーム開発規約。パッケージ構成・レイヤー設計、REST API・例外処理、テストの3分野をまとめたもの。Spring Boot / Java のコードを新規に書く・修正する・レビューする・リファクタリングするときは、明示的に頼まれていなくても必ずこのスキルを使うこと。「APIを作って」「エンドポイント追加」「機能を追加」「Service/Repository/Entityを作って」「例外処理」「エラーハンドリング」「バリデーション」「テストを書いて」「パッケージ構成」「設計を見直して」「このコードをレビューして」などの依頼、および pom.xml や application.yml の変更が対象。
---

# Spring Boot 開発規約

このスキルは、Spring Boot 3.x プロジェクトのコードを誰が書いても（Claude が書いても）同じ形になるようにするための規約です。
規約は3分野に分かれていて、詳細はそれぞれの参照ファイルにあります。**作業に関係する参照ファイルを、コードを書く前に読んでください。** 全部を毎回読む必要はありません。

前提: Spring Boot 3.x / Java 17以上 / Maven / `jakarta.*` パッケージ（`javax.*` は使わない）。

## どの参照ファイルを読むか

| 作業内容 | 読むファイル |
|---|---|
| 新しい機能（Entity〜Controller一式）を作る | `references/architecture.md` → `references/rest-api.md` → `references/rest-api-templates.md` → `references/testing.md` |
| エンドポイントを追加・変更する | `references/rest-api.md`（必要なら `rest-api-templates.md`） |
| 例外処理・エラーレスポンスを作る・直す | `references/rest-api.md` の 5〜7 と `references/rest-api-templates.md` の 2〜4 |
| Entity・Repository・トランザクション・設定値・ログ | `references/architecture.md` |
| テストを書く・直す | `references/testing.md` |
| コードレビュー | 対象に応じて上記すべて。指摘は「どの規約のどの項目に反するか」を添える |

## 全分野に共通する原則

細かいルールは参照ファイルにあるが、迷ったときはこの原則に立ち返る。

1. **機能単位でまとめる。** パッケージは機能（`user/`、`order/`）で切り、その中に Controller・Service・Repository・Entity・dto を置く。
2. **依存は Controller → Service → Repository の一方向。** Controller は HTTP の変換係、Service は業務ロジック、Repository は永続化だけを担当する。
3. **Entity を外に出さない。** API の入出力は `record` の DTO。
4. **例外は投げっぱなしにして一か所で受ける。** Service は業務例外（`BusinessException` の派生）を投げ、`GlobalExceptionHandler` が ProblemDetail 形式に変換する。Controller に try-catch を書かない。
5. **コンストラクタインジェクションのみ。** フィールドは `private final`。
6. **速いテストを多く。** Service は Spring を起動しない単体テスト、Controller は `@WebMvcTest`、DB は Testcontainers。

## 規約と既存コードが食い違う場合

既存プロジェクトのコードがこの規約と違う書き方をしている場合は、勝手に全体を書き換えない。
新しく書く部分は規約に従い、既存部分との違いがあることをユーザーに伝えて、合わせるか直すかを確認する。
大規模な書き換えは差分が大きくなり、レビューも動作確認も難しくなるため。

## 作業後のセルフチェック

コードを出す前に確認する。

- [ ] 機能パッケージの中に置いたか。`common/` に業務ロジックを入れていないか
- [ ] Controller に業務ロジック・try-catch・Repository 呼び出しがないか
- [ ] Entity を返していないか。DTO は record か
- [ ] 作成は 201 + Location、削除は 204 か
- [ ] 新しい業務エラーは `ErrorCode` に追加し、`BusinessException` の派生で投げているか
- [ ] 500 のレスポンスに内部情報が漏れていないか
- [ ] `@Transactional` は Service にあり、参照系は `readOnly = true` か
- [ ] 関連は LAZY か。N+1 になっていないか
- [ ] フィールドインジェクションを使っていないか
- [ ] `javax.*` ではなく `jakarta.*` を import しているか
- [ ] 書いた・変えたコードに対応するテストがあるか（正常系・バリデーション・404 など）
- [ ] ログに個人情報を出していないか
