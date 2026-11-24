package com.example.demo.batch;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.demo.service.LogUploadService;
import com.example.demo.support.FixedClockConfig;

// LogUploadBatch（毎週月曜にログを S3 に送るバッチ）のテスト
// Batch の役目は「今日の日付で Service を呼ぶ」ことと「失敗してもアプリを止めない」ことの2つなので、それを確かめる
// 実際に送る処理は Service のテスト（LogUploadServiceTest）で確かめているので、ここでは Service を偽物にする
@ExtendWith(MockitoExtension.class) // @Mock を使えるようにする
class LogUploadBatchTest {

  @Mock
  LogUploadService logUploadService;// Service の偽物。何も設定しなければ、uploadLogs を呼んでも何もせずに終わる

  private LogUploadBatch logUploadBatch;// テストする本物の Batch

  @BeforeEach // 各テストの前に毎回実行する
  void setUp() {
    // 時計は「2026-11-20 で止まった時計」を渡す（FixedClockConfig はテスト用の道具）
    // 本物の時計だと、テストを実行した日によって「今日」が変わってしまうため
    logUploadBatch = new LogUploadBatch(logUploadService, FixedClockConfig.fixedClock());
  }

  @Test
  void 今日の日付でサービスを呼ぶ() throws IOException {// uploadLogs は IOException を投げるメソッドなので、verify の行のために throws を付ける
    // Given：準備は setUp で済んでいる（今日を 2026-11-20 にした時計と、Service の偽物）

    // When：バッチを動かす（@Scheduled で毎週月曜に呼ばれるメソッドを、テストでは直接呼ぶ）
    logUploadBatch.run();

    // Then：時計の日付（2026-11-20）で、Service の uploadLogs が呼ばれたことを確かめる
    // FixedClockConfig.TODAY は、上の時計と同じ日付を表す定数。時計を変えても、ここを直さなくて済む
    verify(logUploadService).uploadLogs(FixedClockConfig.TODAY);
  }

  @Test
  void Serviceが失敗しても止まらない() throws IOException {
    // Given：Service の偽物を「uploadLogs を呼ぶと失敗する」ように設定する（logs フォルダが読めない、の代わり）
    // uploadLogs は戻り値がない（void）メソッドなので、when(…).thenThrow(…) は使えない。
    // void のメソッドに例外を投げさせるときは、doThrow(例外).when(偽物).メソッド(…) の順に書く
    // any()：引数の日付は何でもよい、という意味
    doThrow(new IOException("フォルダが読めない")).when(logUploadService).uploadLogs(any());

    // When / Then：run() を呼んでも、例外が外に出ないことを確かめる
    // Batch の中の try-catch で受け止めてエラーログを出すので、アプリ（ほかのバッチや画面）は止まらない
    // assertThatCode(() -> 処理)：処理を実行して、その結果（例外が出たかどうか）を確かめる書き方
    assertThatCode(() -> logUploadBatch.run())
        .doesNotThrowAnyException();
  }
}
