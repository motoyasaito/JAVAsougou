# JAVAsougou

## API ドキュメント　(Swagger)

アプリを起動した状態で、ブラウザで次のURLを開くとAPIの確認ができる。

- Swagger UI: http://localhost:8080/swagger-ui.html
- API定義(JSON) : http://localhost:8080/v3/api-docs

### 起動前の準備

初めて起動するときは、`.env.example` をコピーして `.env` を作る。

```
cp .env.example .env
```

- S3 へのログ送信（B003）を動かす場合は、`.env` に AWS のアクセスキーを書く
- 鍵がなくてもアプリは起動する（ログ送信のバッチだけが失敗し、エラーログが出る）
- `.env` は Git に入れない

### 起動方法

docker compose up -d

起動まで30秒ほどかかる。 'docker compose logs -f app'で'Stared DemoApplication'とでたら成功
