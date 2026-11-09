package com.example.demo.batch;

import java.time.LocalDateTime;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.example.demo.service.DeleteCompletedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.ZoneId;

// B001 完了ステータスの月次削除バッチ。実行タイミングは application.properties の batch.delete-completed.cron で決める
@Component // Springに作って管理してもらう印。これがないと@Scheduledも動かない
public class DeleteCompletedBatch {
  private static final Logger log = LoggerFactory.getLogger(DeleteCompletedBatch.class);// このクラス用のログを出す道具
  private final DeleteCompletedService deleteCompletedService;// 削除の処理を任せるService

  public DeleteCompletedBatch(DeleteCompletedService deleteCompletedService) {// Springが起動時にServiceを作って、ここに渡してくれる（コンストラクタインジェクション）
    this.deleteCompletedService = deleteCompletedService;
  }

  // 決まった時間になると、Springが自動でこのメソッドを呼ぶ。Springは何も渡せないので、引数は持てない
  @Scheduled(cron = "${batch.delete-completed.cron}") // ${ }は「application.propertiesの、この名前の値を使う」という意味
  public void run() {
    LocalDateTime threshold = LocalDateTime.now(ZoneId.of("Asia/Tokyo")).minusMonths(1);// 基準日時＝日本時間の今から1か月前
    log.info("完了配送物の削除を開始します。基準日時{}", threshold);// 開始ログ。{}にthresholdが入る
    try {// tryの中で例外が起きたら、そこで止めてcatchに飛ぶ
      deleteCompletedService.deleteCompleted(threshold);// 削除はServiceに任せる。件数のログはService側で出す
      log.info("配送物の削除が完了しました");// 終了ログ
    } catch (Exception e) {// バッチはHTTPリクエストではないので、GlobalExceptionHandlerが動かない。ここで受け取る
      log.error("完了配送物の削除に失敗しました", e);// eを渡すと、どこで何が起きたかまでログに残る
    }
  }
}
