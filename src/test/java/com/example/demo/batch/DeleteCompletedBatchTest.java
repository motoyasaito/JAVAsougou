package com.example.demo.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import com.example.demo.service.DeleteCompletedService;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

// Service をモックにして、Batch が「1か月前の基準日時を作って Service に渡すこと」と「失敗しても止まらないこと」を確認する
// 毎月1日 3:00 に動くこと（@Scheduled）は Spring の機能なので、ここではテストしない（cron を1分ごとにした動作確認で確かめた）
@ExtendWith(MockitoExtension.class) // Mockitoを使う宣言。これがないと@Mockや@InjectMocksが動かない
class DeleteCompletedBatchTest {

  @Mock
  DeleteCompletedService deleteCompletedService;// Serviceの偽物。Batchから見ると「仕事を任せる相手」なので偽物にする

  @InjectMocks
  DeleteCompletedBatch deleteCompletedBatch;// 本物のBatchを作り、コンストラクタに上の偽物を渡す

  @Test
  void 実行_基準日時を1か月前にしてServiceを呼ぶ() {
    // Given run()を呼ぶ直前の「日本時間の今から1か月前」を取っておく
    LocalDateTime before = LocalDateTime.now(ZoneId.of("Asia/Tokyo")).minusMonths(1);

    // When 本物のBatchを動かす。@Scheduledの時間を待たずに、テストから直接呼べる
    deleteCompletedBatch.run();

    // Then Serviceに渡された基準日時が、beforeとafterの間に入っているか確かめる
    LocalDateTime after = LocalDateTime.now(ZoneId.of("Asia/Tokyo")).minusMonths(1);// run()の直後の「1か月前」
    ArgumentCaptor<LocalDateTime> captor = ArgumentCaptor.forClass(LocalDateTime.class);// Serviceに渡された値を捕まえる係
    verify(deleteCompletedService).deleteCompleted(captor.capture());// Serviceが呼ばれたか確かめつつ、渡された値を捕まえる
    // now()はテストのたびに変わるのでぴったりの値では比べられない。Batchがnow()を呼ぶのは必ずbeforeとafterの間なので、その範囲に入っていればOK
    assertThat(captor.getValue()).isBetween(before, after);// isBetween(A, B)は「A以上B以下」。1か月前でなければ（例：minusDaysにした）ここで失敗する
  }

  @Test
  void 実行_削除に失敗しても例外を外に出さない() {
    // Given Serviceの削除でDBエラーが起きた状態を作る。deleteCompletedはvoidなので、when(...)ではなくdoThrowを使う
    doThrow(new RuntimeException("DBエラー"))
        .when(deleteCompletedService).deleteCompleted(any());// どんな基準日時で呼ばれても、RuntimeExceptionを投げると教える

    // When/Then run()を呼んでも例外が外に出ないか確かめる
    // Batchのtry-catchで受け取り、エラーログを出して終わるのが正しい動き。誰かがtry-catchを消すと、ここで失敗する
    assertThatCode(() -> deleteCompletedBatch.run())// assertThatThrownByの仲間。中の処理を動かして結果を調べる
        .doesNotThrowAnyException();// 「何の例外も出なかった」ことを確かめる（assertThatThrownByとは反対）
  }
}
