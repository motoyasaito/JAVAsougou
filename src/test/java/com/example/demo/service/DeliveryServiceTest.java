package com.example.demo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;

import com.example.demo.dto.AssignDriverRequest;
import com.example.demo.dto.CreateDeliveryRequest;

import com.example.demo.dto.DeliveryResponse;
import com.example.demo.dto.PageResponse;
import com.example.demo.entity.Delivery;
import com.example.demo.exception.ConflictException;
import com.example.demo.exception.NotFoundException;
import com.example.demo.mapper.DeliveryMapper;
import com.example.demo.mapper.UserMapper;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

// Mapper をモックにして、Service のロジック（日付の補完・offset の計算・ページ数・DTO への変換）だけを確認する
@ExtendWith(MockitoExtension.class)
class DeliveryServiceTest {

  @Mock
  DeliveryMapper deliveryMapper;// マッパーの偽物を作る。

  @Mock
  TrackingNumberGenerator trackingNumberGenerator;// 伝票番号を作る道具の偽物。本物は毎回ランダムなので、決まった番号を返させる

  @Mock
  UserMapper userMapper;

  @InjectMocks
  DeliveryService deliveryService;// 本物のServiceを作り、コンストラクタに上の偽物3つ（MapperとGenerator）を渡す

  // ---------- 管理者の配送物一覧（findAll） ----------

  @Test
  void 管理者一覧_配送物の一覧をDTOに変換して返す() {
    // Given マッパーが返すデータ（エンティティ）を1件作る。テストで確かめたい項目だけ値を入れる。
    Delivery delivery = new Delivery();
    delivery.setId(1L);
    delivery.setTrackingNumber("123456789012");
    when(deliveryMapper.findAll(10, 0L)).thenReturn(List.of(delivery));// findAll(10, 0)と呼ばれたらこの1件を返すと教える。
    when(deliveryMapper.count()).thenReturn(1L);// count()と呼ばれたら1を返すと教える。LはlongのL

    // When
    PageResponse<DeliveryResponse> result = deliveryService.findAll(0, 10);// 本物のサービスをpage=0,size=10で呼ぶ

    // Then
    // assertThat(実際の値).isEqualTo(期待する値)は「同じでなければ失敗」という意味。DTOに変換されて中身が引き継がれているか確かめる。
    assertThat(result.content()).hasSize(1);
    assertThat(result.content().get(0).id()).isEqualTo(1L);
    assertThat(result.content().get(0).trackingNumber()).isEqualTo("123456789012");
    assertThat(result.totalElements()).isEqualTo(1L);
  }

  @Test
  void 管理者一覧_ページ番号と件数からoffsetとページ数を計算する() {
    // Given
    when(deliveryMapper.count()).thenReturn(25L);// 全部で25件あるときに、3ページ目（page=2)を10件ずつでとる

    // When
    PageResponse<DeliveryResponse> result = deliveryService.findAll(2, 10);

    // Then
    verify(deliveryMapper).findAll(10, 20L);// verifyは「この引数で呼ばれたか」を確かめるもの。2*10=20なので、offsetが20で呼ばれていれば成功。
    assertThat(result.page()).isEqualTo(2);
    assertThat(result.size()).isEqualTo(10);
    assertThat(result.totalPages()).isEqualTo(3);// 25件を10件ずつに分けると3ページになる。
  }

  @Test
  void 管理者一覧_ページ番号が大きくてもoffsetが負の数にならない() {
    // When
    deliveryService.findAll(Integer.MAX_VALUE, 100);// Integer.MAX_VALUEはintで表せる一番大きな数。もし誰かが（long）を消してしまうと、offsetがマイナスで呼ばれテスト失敗に気づける

    // Then
    verify(deliveryMapper).findAll(100, (long) Integer.MAX_VALUE * 100);
  }

  @Test
  void 管理者一覧_配送物がなければ空の一覧を返す() {
    // When
    PageResponse<DeliveryResponse> result = deliveryService.findAll(0, 10);

    // Then
    assertThat(result.content()).isEmpty();
    assertThat(result.totalElements()).isZero();
    assertThat(result.totalPages()).isZero();
  }

  // ---------- ドライバーの配達一覧（findByDriver） ----------

  @Test
  void ドライバー一覧_日付を指定したらその日付で配達一覧を取得する() {
    // Given
    LocalDate date = LocalDate.of(2026, 10, 26);
    Delivery delivery = new Delivery();
    delivery.setTrackingNumber("123456789012");
    when(deliveryMapper.findByDriverId(1L, date, 10, 0L)).thenReturn(List.of(delivery));
    when(deliveryMapper.countByDriverId(1L, date)).thenReturn(1L);

    // When
    PageResponse<DeliveryResponse> result = deliveryService.findByDriver(1L, date, 0, 10);

    // Then
    assertThat(result.content()).hasSize(1);
    assertThat(result.content().get(0).trackingNumber()).isEqualTo("123456789012");
    assertThat(result.totalElements()).isEqualTo(1L);
  }

  @Test
  void ドライバー一覧_日付を省略したら日本時間の今日の配達一覧を取得する() {
    // Given
    LocalDate today = LocalDate.now(ZoneId.of("Asia/Tokyo"));
    when(deliveryMapper.findByDriverId(1L, today, 10, 0L)).thenReturn(List.of());
    // When
    deliveryService.findByDriver(1L, null, 0, 10);
    // Then
    verify(deliveryMapper).findByDriverId(1L, today, 10, 0L);
    verify(deliveryMapper).countByDriverId(1L, today);
  }

  @Test
  void ドライバー一覧_ページ番号と件数からoffsetとページ数を計算する() {
    // Given
    LocalDate date = LocalDate.of(2026, 10, 26);
    when(deliveryMapper.countByDriverId(1L, date)).thenReturn(25L);
    when(deliveryMapper.findByDriverId(1L, date, 10, 20L)).thenReturn(List.of());
    // When
    PageResponse<DeliveryResponse> result = deliveryService.findByDriver(1L, date, 2, 10);

    // Then
    verify(deliveryMapper).findByDriverId(1L, date, 10, 20L);
    assertThat(result.page()).isEqualTo(2);
    assertThat(result.size()).isEqualTo(10);
    assertThat(result.totalPages()).isEqualTo(3);
  }

  @Test
  void ドライバー一覧_ページ番号が大きくてもoffsetが負の数にならない() {
    // Given
    LocalDate date = LocalDate.of(2026, 10, 26);
    when(deliveryMapper.findByDriverId(1L, date, 100, (long) Integer.MAX_VALUE * 100)).thenReturn(List.of());
    // When
    deliveryService.findByDriver(1L, date, Integer.MAX_VALUE, 100);
    // Then
    verify(deliveryMapper).findByDriverId(1L, date, 100, (long) Integer.MAX_VALUE * 100);
  }

  @Test
  void ドライバー一覧_該当する配達がなければ空の一覧を返す() {
    // Given
    LocalDate date = LocalDate.of(2026, 10, 26);
    when(deliveryMapper.findByDriverId(1L, date, 10, 0L)).thenReturn(List.of());
    // When
    PageResponse<DeliveryResponse> result = deliveryService.findByDriver(1L, date, 0, 10);

    // Then
    assertThat(result.content()).isEmpty();
    assertThat(result.totalElements()).isZero();
    assertThat(result.totalPages()).isZero();
  }

  // ---------- 配送物の登録（create） ----------

  @Test
  void 登録_伝票番号をセットして登録し登録した内容を返す() {
    // Given
    when(trackingNumberGenerator.generate()).thenReturn("123456789012");// generate()と呼ばれたら、決まった番号を返すと教える
    when(deliveryMapper.existsByTrackingNumber("123456789012")).thenReturn(false);// この番号はまだ使われていない（false）と教える
    // insert()が呼ばれたら、下の { } の中の処理を実行する。any(Delivery.class)は「どんなDeliveryが来ても」という意味
    when(deliveryMapper.insert(any(Delivery.class))).thenAnswer(invocation -> {
      Delivery inserted = invocation.getArgument(0);// insert()に渡された1つ目の引数（Serviceが作ったdelivery）を取り出す
      inserted.setId(1L);// 本物のinsertはDBが採番したidを入れてくれるので、偽物でも同じようにidを入れる
      return 1;// 本物と同じく「1件登録した」を返す
    });
    Delivery saved = new Delivery();// findById()で返す「DBから取り直したデータ」を作る
    saved.setId(1L);
    saved.setTrackingNumber("123456789012");
    saved.setStatus("PREPARING");// statusはDBが入れる値。取り直したデータにだけ入っている
    when(deliveryMapper.findById(1L)).thenReturn(Optional.of(saved));// findById(1)と呼ばれたら、上のデータを箱（Optional）に入れて返すと教える

    // When
    DeliveryResponse result = deliveryService.create(createRequest());// 本物のServiceで登録を実行する

    // Then
    ArgumentCaptor<Delivery> captor = ArgumentCaptor.forClass(Delivery.class);// メソッドに渡された引数を捕まえる道具を作る
    verify(deliveryMapper).insert(captor.capture());// insert()が呼ばれたか確かめ、そのとき渡されたdeliveryを捕まえる
    assertThat(captor.getValue().getTrackingNumber()).isEqualTo("123456789012");// 捕まえたdeliveryに、作った伝票番号がセットされているか
    assertThat(captor.getValue().getName()).isEqualTo("山田太郎");// リクエストの内容がdeliveryに詰め替えられているか
    assertThat(result.id()).isEqualTo(1L);// 登録で入ったidが返ってくるか
    assertThat(result.status()).isEqualTo("PREPARING");// PREPARINGが返る＝findByIdで取り直したデータを返しているか
  }

  @Test
  void 登録_伝票番号が使われていたら作り直す() {
    // Given 1回目は使用済みの番号、2回目は未使用の番号を返す
    when(trackingNumberGenerator.generate()).thenReturn("111111111111", "222222222222");// 値を2つ並べると、1回目は1つ目、2回目は2つ目を返す
    when(deliveryMapper.existsByTrackingNumber("111111111111")).thenReturn(true);// 1回目の番号は使用済み（true）→ whileで作り直しになる
    when(deliveryMapper.existsByTrackingNumber("222222222222")).thenReturn(false);// 2回目の番号は未使用（false）→ whileを抜ける
    when(deliveryMapper.insert(any(Delivery.class))).thenAnswer(invocation -> {// テスト1と同じく、偽物のinsertでidを入れる
      Delivery inserted = invocation.getArgument(0);
      inserted.setId(1L);
      return 1;
    });
    when(deliveryMapper.findById(1L)).thenReturn(Optional.of(new Delivery()));// このテストでは返す中身は確かめないので、空のDeliveryでよい

    // When
    deliveryService.create(createRequest());

    // Then 作り直した2回目の番号で登録されている
    ArgumentCaptor<Delivery> captor = ArgumentCaptor.forClass(Delivery.class);
    verify(deliveryMapper).insert(captor.capture());
    assertThat(captor.getValue().getTrackingNumber()).isEqualTo("222222222222");// 使用済みの1回目ではなく、2回目の番号でinsertされているか
  }

  // ---------- 配送物の詳細（findByTrackingNumber） ----------

  @Test
  void 詳細_伝票番号で配送物を取得してDTOに変換して返す() {
    // Given マッパーが返すデータを1件作り、「この伝票番号で呼ばれたら、箱（Optional）に入れて返す」と教える
    Delivery delivery = new Delivery();// DBから読んだふりをするための、空の入れ物（1行分）を作る
    delivery.setId(1L);// 確かめたい項目だけ値を入れる。idは1
    delivery.setTrackingNumber("123456789012");// 伝票番号
    delivery.setName("山田太郎");// お届け先の氏名
    when(deliveryMapper.findByTrackingNumber("123456789012")).thenReturn(Optional.of(delivery));// この番号で呼ばれたら、上のdeliveryを箱（Optional）に入れて返すと教える

    // When
    DeliveryResponse result = deliveryService.findByTrackingNumber("123456789012");// 本物のServiceで詳細を取得する

    // Then DTOに変換されて、中身が引き継がれているか確かめる
    assertThat(result.id()).isEqualTo(1L);// idが引き継がれているか
    assertThat(result.trackingNumber()).isEqualTo("123456789012");// 伝票番号が引き継がれているか
    assertThat(result.name()).isEqualTo("山田太郎");// 氏名が引き継がれているか
  }

  @Test
  void 詳細_伝票番号が見つからなければ404の例外を投げる() {
    // Given 「この伝票番号で呼ばれたら、空の箱を返す」と教える（DBに該当データがない状態）
    when(deliveryMapper.findByTrackingNumber("999999999999")).thenReturn(Optional.empty());// Optional.empty()は中身がない箱。これでServiceのorElseThrowが例外を投げる道に進む

    // When / Then assertThatThrownByの中で呼んだ処理が、NotFoundExceptionを投げるか確かめる
    assertThatThrownBy(() -> deliveryService.findByTrackingNumber("999999999999"))// () ->で処理を包んで渡すと、例外が出てもテストが止まらず、投げられた例外を調べられる
        .isInstanceOf(NotFoundException.class)// 投げられた例外がNotFoundExceptionか確かめる
        .hasMessage("該当する配送物が見つかりません");// メッセージも確かめる。GlobalExceptionHandlerがこの文字をレスポンスに入れるため
  }

  // テストで使う登録リクエストを作る（2つのテストで同じものを使うのでメソッドにまとめる）
  private CreateDeliveryRequest createRequest() {
    // recordは new クラス名(値, …) で作れる。値はrecordに書いた項目の順番（氏名・住所・電話・メール・種類・配達日・時間帯）で並べる
    return new CreateDeliveryRequest("山田太郎", "東京都千代田区1-1", "09012345678", "taro@example.com", "NORMAL",
        LocalDate.of(2026, 10, 28), "午前中");
  }

  // ---------- 担当ドライバーの割り振り（assignDriver） ----------
  @Test
  void 割り振り_発送準備中なら担当ドライバーを更新する() {
    // Given
    Delivery delivery = new Delivery();
    delivery.setId(1L);
    delivery.setStatus("PREPARING");
    when(deliveryMapper.findById(1L)).thenReturn(Optional.of(delivery));
    when(userMapper.existsDriverById(2L)).thenReturn(true);

    // When
    deliveryService.assignDriver(1L, new AssignDriverRequest(2L));

    // Then
    verify(deliveryMapper).updateDriverById(1L, 2L);
  }

  @Test
  void 配送物が見つからない場合404を投げる() {
    // Given
    when(deliveryMapper.findById(1L)).thenReturn(Optional.empty());

    // When/Then
    assertThatThrownBy(() -> deliveryService.assignDriver(1L, new AssignDriverRequest(2L)))
        .isInstanceOf(NotFoundException.class)
        .hasMessage("該当する配送物が見つかりません");
  }

  @Test
  void ドライバーではない場合404を投げる() {
    // Given
    Delivery delivery = new Delivery();
    delivery.setId(1L);
    delivery.setStatus("PREPARING");
    when(deliveryMapper.findById(1L)).thenReturn(Optional.of(delivery));
    when(userMapper.existsDriverById(1L)).thenReturn(false);

    assertThatThrownBy(() -> deliveryService.assignDriver(1, new AssignDriverRequest(1L)))
        .isInstanceOf(NotFoundException.class)
        .hasMessage("該当するドライバーが見つかりません");
  }

  @Test
  void 発送準備中でない場合409を投げる() {
    // Given
    Delivery delivery = new Delivery();
    delivery.setStatus("IN TRANSIT");
    when(deliveryMapper.findById(1L)).thenReturn(Optional.of(delivery));
    when(userMapper.existsDriverById(2L)).thenReturn(true);

    // When/Then
    assertThatThrownBy(() -> deliveryService.assignDriver(1L, new AssignDriverRequest(2L)))
        .isInstanceOf(ConflictException.class)
        .hasMessage("発送準備中の配送物のみ担当を割り振れます");
  }
}
