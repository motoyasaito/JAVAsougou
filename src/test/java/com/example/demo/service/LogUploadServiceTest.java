package com.example.demo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

// LogUploadService（ログファイルを S3 に送る処理）のテスト
// 本物の S3 には送らず、S3Client を偽物（モック）にして、「どのファイルを・どこに送ろうとしたか」と
// 「手元のファイルが消えたか・残ったか」を確かめる
@ExtendWith(MockitoExtension.class) // @Mock を使えるようにする（Mockito を JUnit で使うための決まった書き方）
class LogUploadServiceTest {

  @Mock
  S3Client s3Client;// 本物のS3には送らない偽物。何も設定しなければ、putObject を呼んでも何もせずに終わる

  @TempDir // テストのたびに空の一時フォルダを作ってくれる。テストが終わったら自動で消える
  Path logDir;// 本物の logs フォルダの代わり。本物を使うと、テストで本物のログファイルが消えてしまうため

  private LogUploadService logUploadService;// テストする本物の Service

  @BeforeEach // 各テストの前に毎回実行する
  void setUp() {
    // @Value（application.properties から値を受け取る仕組み）はこのテストでは働かないので、自分で new して値を渡す
    // バケット名はテスト用の名前、フォルダは上の一時フォルダにする
    logUploadService = new LogUploadService(s3Client, "test-bucket", logDir);
  }

  @Test
  void 今日より前のファイルを送る() throws IOException {// Files.writeString が IOException を投げる可能性があるので throws を付ける
    // Given：一時フォルダに、今日（11/24）より前の日付のログファイルを作る
    // logDir.resolve("ファイル名") で、フォルダの中のファイルの場所を作る。writeString で中身を書いてファイルを作る
    Path oldFile = Files.writeString(logDir.resolve("app.2026-11-09.log"), "テスト");

    // When：基準日を 2026-11-24 にして、送る処理を呼ぶ
    logUploadService.uploadLogs(LocalDate.of(2026, 11, 24));

    // Then
    // ArgumentCaptor：偽物のメソッドに「何が渡されたか」を捕まえる道具。ここでは putObject に渡された設定（PutObjectRequest）を捕まえる
    ArgumentCaptor<PutObjectRequest> captor = ArgumentCaptor.forClass(PutObjectRequest.class);
    // putObject が1回呼ばれたことを確かめながら、1つ目の引数を captor で捕まえる。2つ目の引数（送る中身）は何でもよいので any にする
    verify(s3Client).putObject(captor.capture(), any(RequestBody.class));
    // 捕まえた設定から、置き場所（キー）とバケットを取り出して確かめる。キーは「種類/年/月/ファイル名」の形
    assertThat(captor.getValue().key()).isEqualTo("app/2026/11/app.2026-11-09.log");
    assertThat(captor.getValue().bucket()).isEqualTo("test-bucket");
    // 送れたら手元のファイルは消える。Files.exists はファイルがあれば true なので、false（消えている）を確かめる
    assertThat(Files.exists(oldFile)).isFalse();
  }

  @Test
  void 今日のファイルは送らない() throws IOException {
    // Given：基準日（11/24）と同じ日のファイルを作る。今日のファイルはまだ書き込み中なので送らない決まり
    Path oldFile = Files.writeString(logDir.resolve("app.2026-11-24.log"), "テスト");

    // When
    logUploadService.uploadLogs(LocalDate.of(2026, 11, 24));

    // Then
    // never()：一度も呼ばれていないことを確かめる。送っていないので、captor で捕まえるものもない
    verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    // 送っていないので、ファイルは残っている（true）
    assertThat(Files.exists(oldFile)).isTrue();
  }

  @Test
  void 関係ないファイルは送らない() throws IOException {
    // Given：ログではない名前のファイルを作る。「app.日付.log」「api-access.日付.log」の形でないものは送らない決まり
    Path memoFile = Files.writeString(logDir.resolve("memo.txt"), "テストメモ");

    // When
    logUploadService.uploadLogs(LocalDate.of(2026, 11, 24));

    // Then：一度も送らず、ファイルも残っている
    verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    assertThat(Files.exists(memoFile)).isTrue();

  }

  @Test
  void apiAccessログはapiaccessの場所に送る() throws IOException {
    // Given：もう1種類のログ（APIアクセスログ）のファイルを作る
    // 1つ目のテストは app のファイルだけなので、種類ごとに置き場所を分けられているかは、こちらで確かめる
    Path oldFile = Files.writeString(logDir.resolve("api-access.2026-11-09.log"), "テスト");

    // When
    logUploadService.uploadLogs(LocalDate.of(2026, 11, 24));

    // Then：置き場所（キー）の最初が api-access になっている
    ArgumentCaptor<PutObjectRequest> captor = ArgumentCaptor.forClass(PutObjectRequest.class);
    verify(s3Client).putObject(captor.capture(), any(RequestBody.class));
    assertThat(captor.getValue().key()).isEqualTo("api-access/2026/11/api-access.2026-11-09.log");
    assertThat(captor.getValue().bucket()).isEqualTo("test-bucket");
    assertThat(Files.exists(oldFile)).isFalse();
  }

  @Test
  void ファイルが送れなかったら残す() throws IOException {
    // Given：送る対象のファイルを作る
    Path oldFile = Files.writeString(logDir.resolve("api-access.2026-11-09.log"), "テスト");
    // 偽物の S3Client を「putObject を呼ぶと失敗する」ように設定する（通信できない・権限がない、の代わり）
    // when(○○).thenThrow(例外)：○○ が呼ばれたら、その例外を投げる。偽物の設定は、呼ぶ（When）より前にしておく
    // SdkException は new ではなく builder() で作る決まり（AWS SDK の書き方）
    when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
        .thenThrow(SdkException.builder().message("送れない").build());

    // When：Service の中で例外を受け止めるので、テストは例外で止まらずに次の行に進む
    logUploadService.uploadLogs(LocalDate.of(2026, 11, 24));

    // Then：送れなかったファイルは消さずに残す（次回もう一度送るため）
    assertThat(Files.exists(oldFile)).isTrue();

  }
}
