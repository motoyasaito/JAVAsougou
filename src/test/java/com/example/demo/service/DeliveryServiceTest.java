package com.example.demo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.demo.dto.DeliveryResponse;
import com.example.demo.dto.PageResponse;
import com.example.demo.entity.Delivery;
import com.example.demo.mapper.DeliveryMapper;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
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

  @InjectMocks
  DeliveryService deliveryService;// 本物のサービスを使い、コンストラクタに上の偽物マッパーを渡す。

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
}
