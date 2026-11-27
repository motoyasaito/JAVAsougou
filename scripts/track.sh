#!/bin/bash
# 1行目（シバン）：このファイルを bash で動かすという宣言。必ず1行目に書く

#引数の前後にある半角スペースとタブを取り除いて、trackingNumber に入れる
trackingNumber=$(printf '%s' "$1" | sed -E 's/^[ \t]+|[ \t]+$//g') #$1は1つ目の引数。printf '%s'で引数をそのまま出し、sedで「先頭の空白（^[ \t]+）か末尾の空白（[ \t]+$）」を空に置きかえる（＝消す）

#引数が1つじゃない場合、説明を表示する
if [ $# -ne 1 ]; then #-neは等しくない（not equal）。数を比べる時に使う。「引数が一つでないなら」
  echo "使い方: ./scripts/track.sh 配送番号" >&2 #>&2で標準エラーに出す（成功したときの結果と分けるため）
  exit 1 #ここでスクリプトを終わらせる。1は「入力の誤り」の終了コード
fi #if文の終わりの印

#伝票番号が12桁の数字以外だった場合、エラー文を表示する
if [[ ! "$trackingNumber" =~ ^[0-9]{12}$ ]]; then #[]の強化版。正規表現は[[]]でないと使えない。=~は「正規表現に一致するか」、!で「一致しなければ」。^[0-9]{12}$は最初から最後まで数字がちょうど12個
  echo "配送番号は12桁の数字で入力してください" >&2
  exit 1
fi

#psqlがこのPCに入っていない場合、エラー文を表示する
if ! command -v psql > /dev/null 2>&1; then #command -vでpsqlの場所を探す。!で「見つからなければ」。> /dev/null 2>&1は、探した結果の表示を捨てる（画面に出さない）
  echo "psql コマンドが見つかりません" >&2
  exit 2 #2は「データベース側の問題」の終了コード
fi

#データベースの接続先を決める。環境変数があればそれを使い、なければdocker-compose.ymlのpostgresと同じ値を使う
export PGHOST="${PGHOST:-localhost}" #${変数名:-初期値}は「変数に値があればそれ、なければ初期値」。exportでこのあと呼ぶpsqlにも渡す
export PGPORT="${PGPORT:-5433}" #DockerのPostgresは、PCの5433番につないでいる
export PGUSER="${PGUSER:-postgres}"
export PGDATABASE="${PGDATABASE:-sougou}"
export PGPASSWORD="${PGPASSWORD:-postgres}" #psqlはPGPASSWORDを自分で読むので、コマンドにパスワードを書かずに済む（画面に出さない）

#配送番号で配送物を1件探し、結果（例：100000000001|PREPARING|2026-10-23|午前中）を result に入れる
#お届け予定日と時間帯の決め方は DeliveryMapper.findStatusByTrackingNumber と同じ（再配達があれば最新の再配達のもの）
#-w：パスワードを聞かれて止まらないように / -tA：値だけを|区切りの1行で出す / -v ON_ERROR_STOP=1：SQLのエラーで失敗にする
#-v tracking_number：配送番号はpsqlの変数で渡す（SQLに文字をつなげて埋め込まない） / 2>/dev/null：エラー文に接続先が出るので捨てる
#<<'SQL' から「SQL」の行までをpsqlに渡す（SQLの中のコメントは -- で書く）
result=$(psql -X -w -q -tA -v ON_ERROR_STOP=1 -v tracking_number="$trackingNumber" 2>/dev/null <<'SQL'
      SELECT d.tracking_number, d.status, -- 個人情報（氏名・住所・電話番号・メール）の列は取らない
             COALESCE(r.redelivery_date, d.delivery_date) AS delivery_date, -- COALESCE(a, b)：aがNULLならbを使う。再配達の日付がなければ最初の配達日
             CASE WHEN r.id IS NULL THEN d.delivery_time ELSE r.redelivery_time END AS delivery_time -- 再配達がなければ最初の時間帯。日付と時間帯を同じ行から取って、ちぐはぐにしない
      FROM deliveries d
      LEFT JOIN LATERAL ( -- 配送物1件ごとに、カッコの中のSELECT（最新の再配達を1行だけ取る）を実行してつなげる。LEFTなので再配達がない配送物も消えない
          SELECT id, redelivery_date, redelivery_time
          FROM redeliveries
          WHERE delivery_id = d.id
          ORDER BY redelivery_date DESC NULLS LAST, id DESC -- 再配達日が新しい順（NULLは後ろ）。同じ日ならidが大きい順
          LIMIT 1 -- いちばん上の1件だけ
      ) r ON true
      WHERE d.tracking_number = :'tracking_number'; -- :'変数名' と書くと、psqlが'で囲んで安全に入れる
SQL
)

#psqlが失敗した（接続できない、SQLのエラー）場合、エラー文を表示する
#※$? は「直前のコマンド」の終了コードなので、psqlのすぐ次に書く。間に別のコマンドを入れると、そのコマンドの終了コードに変わってしまう
if [ $? -ne 0 ]; then #$?はpsqlの終了コード。0なら成功、0以外なら失敗
  echo "データベースに接続できません" >&2
  exit 2
fi

#見つからなかった（0件）場合、エラー文を表示する
if [ -z "$result" ]; then #-zは空かどうかを調べる。resultが空＝1件も見つからなかった
  echo "該当する配送物が見つかりません" >&2
  exit 1
fi

#2件以上見つかった場合、エラーにする（tracking_numberはUNIQUEなので普通は1件。2件以上ならデータがおかしい）
count=$(printf '%s\n' "$result" | wc -l) #wc -lで結果の行数（＝件数）を数える
if [ "$count" -ne 1 ]; then
  echo "データベースに接続できません" >&2
  exit 2
fi

#検索結果の1行（例：100000000001|PREPARING|2026-10-23|午前中）を、|で4つに分けて変数に入れる
IFS='|' read -r trackingNumber status deliveryDate deliveryTime <<< "$result" #IFS='|'で区切りを|にする（この行のときだけ）。readで左から順に4つの変数に入れる。-rは\をそのまま扱う。<<<でresultの中身をreadに渡す

#DBの状態（PREPARINGなど）を、画面のstatusBadgeと同じ日本語にする
case "$status" in #caseは値によって分ける（Javaのswitchと同じ）
  "PREPARING") statusLabel="発送準備中" ;; #;;はそのときの処理の終わり（Javaのbreak）
  "IN TRANSIT") statusLabel="配達中" ;; #空白が入っているので"で囲む
  "DELIVERED") statusLabel="配達完了" ;;
  "ABSENT") statusLabel="不在" ;;
  *) statusLabel="$status" ;; #*)はどれにも当てはまらないとき（Javaのdefault）。DBの値をそのまま出す
esac #case文の終わりの印（caseを逆から書いたもの）

#見つかったときは、標準出力にこの4行だけを出す。NULLの日付・時間帯は空のまま「お届け予定日: 」のように出す
#文言は講師の確認のうえ「配送番号」にそろえた（概要の例は「伝票番号」）
echo "配送番号: ${trackingNumber}"
echo "状態: ${statusLabel}"
echo "お届け予定日: ${deliveryDate}"
echo "時間帯: ${deliveryTime}"
