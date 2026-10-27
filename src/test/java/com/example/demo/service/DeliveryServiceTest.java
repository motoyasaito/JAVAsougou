package com.example.demo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import com.example.demo.dto.CreateDeliveryRequest;

import com.example.demo.dto.DeliveryResponse;
import com.example.demo.dto.PageResponse;
import com.example.demo.entity.Delivery;
import com.example.demo.mapper.DeliveryMapper;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

// Mapper をモックにして、Service のロジック（日付の補完・offset の計算・ページ数）だけを確認する
@ExtendWith(MockitoExtension.class)
class DeliveryServiceTest {

  @Mock
  DeliveryMapper deliveryMapper;

  @Mock
  TrackingNumberGenerator trackingNumberGenerator;// 伝票番号を作る道具の偽物。本物は毎回ランダムなので、決まった番号を返させる

  @InjectMocks
  DeliveryService deliveryService;// 本物のServiceを作り、コンストラクタに上の偽物2つ（MapperとGenerator）を渡す

  @Test
  void 日付を指定したらその日付で配達一覧を取得する() {
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
  void 日付を省略したら日本時間の今日の配達一覧を取得する() {
    // Given
    LocalDate today = LocalDate.now(ZoneId.of("Asia/Tokyo"));

    // When
    deliveryService.findByDriver(1L, null, 0, 10);

    // Then
    verify(deliveryMapper).findByDriverId(1L, today, 10, 0L);
    verify(deliveryMapper).countByDriverId(1L, today);
  }

  @Test
  void ページ番号と件数からoffsetとページ数を計算する() {
    // Given
    LocalDate date = LocalDate.of(2026, 10, 26);
    when(deliveryMapper.countByDriverId(1L, date)).thenReturn(25L);

    // When
    PageResponse<DeliveryResponse> result = deliveryService.findByDriver(1L, date, 2, 10);

    // Then
    verify(deliveryMapper).findByDriverId(1L, date, 10, 20L);
    assertThat(result.page()).isEqualTo(2);
    assertThat(result.size()).isEqualTo(10);
    assertThat(result.totalPages()).isEqualTo(3);
  }

  @Test
  void ページ番号が大きくてもoffsetが負の数にならない() {
    // Given
    LocalDate date = LocalDate.of(2026, 10, 26);

    // When
    deliveryService.findByDriver(1L, date, Integer.MAX_VALUE, 100);

    // Then
    verify(deliveryMapper).findByDriverId(1L, date, 100, (long) Integer.MAX_VALUE * 100);
  }

  @Test
  void 該当する配達がなければ空の一覧を返す() {
    // Given
    LocalDate date = LocalDate.of(2026, 10, 26);

    // When
    PageResponse<DeliveryResponse> result = deliveryService.findByDriver(1L, date, 0, 10);

    // Then
    assertThat(result.content()).isEmpty();
    assertThat(result.totalElements()).isZero();
    assertThat(result.totalPages()).isZero();
  }

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

  // テストで使う登録リクエストを作る（2つのテストで同じものを使うのでメソッドにまとめる）
  private CreateDeliveryRequest createRequest() {
    // recordは new クラス名(値, …) で作れる。値はrecordに書いた項目の順番（氏名・住所・電話・メール・種類・配達日・時間帯）で並べる
    return new CreateDeliveryRequest("山田太郎", "東京都千代田区1-1", "09012345678", "taro@example.com", "NORMAL",
        LocalDate.of(2026, 10, 28), "午前中");
  }
}
