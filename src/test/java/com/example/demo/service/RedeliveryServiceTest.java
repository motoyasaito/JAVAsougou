package com.example.demo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import com.example.demo.dto.CreateRedeliveryRequest;
import com.example.demo.dto.AssignDriverRequest;

import com.example.demo.dto.RedeliveryResponse;
import com.example.demo.entity.Delivery;
import com.example.demo.entity.Redelivery;
import com.example.demo.mapper.DeliveryMapper;
import com.example.demo.mapper.RedeliveryMapper;
import com.example.demo.mapper.UserMapper;
import com.example.demo.exception.NotFoundException;
import com.example.demo.exception.BadRequestException;
import com.example.demo.exception.ConflictException;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RedeliveryServiceTest {

  @Mock
  RedeliveryMapper redeliveryMapper;

  @Mock
  DeliveryMapper deliveryMapper;

  @Mock
  UserMapper userMapper;

  @InjectMocks
  RedeliveryService redeliveryService;

  @Test
  void 登録_条件を満たせば再配達を登録してstatusをPREPARINGに戻す() {
    // Given
    when(deliveryMapper.findByTrackingNumber("123456789012")).thenReturn(Optional.of(absentDelivery()));

    // When
    CreateRedeliveryRequest request = createRedeliveryRequest();
    RedeliveryResponse result = redeliveryService.create(request);

    // Then
    ArgumentCaptor<Redelivery> captor = ArgumentCaptor.forClass(Redelivery.class);
    verify(redeliveryMapper).insert(captor.capture());
    assertThat(captor.getValue().getDeliveryId()).isEqualTo(1L);
    assertThat(captor.getValue().getRedeliveryDate()).isEqualTo(request.redeliveryDate());
    assertThat(captor.getValue().getRedeliveryTime()).isEqualTo(request.redeliveryTime());
    verify(deliveryMapper).updateStatus(1L, "PREPARING");
    assertThat(result.trackingNumber()).isEqualTo("123456789012");
  }

  @Test
  void 登録_伝票番号が見つからなければ404の例外を投げる() {
    // Given
    when(deliveryMapper.findByTrackingNumber("123456789012")).thenReturn(Optional.empty());

    // When/Then
    assertThatThrownBy(() -> redeliveryService.create(createRedeliveryRequest()))
        .isInstanceOf(NotFoundException.class);
  }

  @Test
  void 登録_電話番号が違う場合404の例外を投げる() {
    // Given
    Delivery delivery = absentDelivery();
    delivery.setCallNumber("08011112222");
    when(deliveryMapper.findByTrackingNumber("123456789012")).thenReturn(Optional.of(delivery));

    // When / Then
    assertThatThrownBy(() -> redeliveryService.create(createRedeliveryRequest()))
        .isInstanceOf(NotFoundException.class);
  }

  @Test
  void 登録_ステータスが不在でない場合409の例外を投げる() {
    // Given
    Delivery delivery = absentDelivery();
    delivery.setStatus("DELIVERED");
    when(deliveryMapper.findByTrackingNumber("123456789012")).thenReturn(Optional.of(delivery));

    // When/Then
    assertThatThrownBy(() -> redeliveryService.create(createRedeliveryRequest()))
        .isInstanceOf(ConflictException.class);
  }

  @Test
  void 登録_再配達が今日の場合400を投げる() {

    // Given
    when(deliveryMapper.findByTrackingNumber("123456789012")).thenReturn(Optional.of(absentDelivery()));
    LocalDate today = LocalDate.now(ZoneId.of("Asia/Tokyo"));
    CreateRedeliveryRequest request = new CreateRedeliveryRequest("123456789012", "08055551122", today, "午前中");

    // When/Then
    assertThatThrownBy(() -> redeliveryService.create(request))
        .isInstanceOf(BadRequestException.class);
  }

  private CreateRedeliveryRequest createRedeliveryRequest() {
    return new CreateRedeliveryRequest("123456789012", "08055551122",
        LocalDate.now(ZoneId.of("Asia/Tokyo")).plusDays(1), "午前中");
  }

  private Delivery absentDelivery() {
    Delivery delivery = new Delivery();
    delivery.setId(1L);
    delivery.setTrackingNumber("123456789012");
    delivery.setCallNumber("08055551122");
    delivery.setStatus("ABSENT");
    return delivery;
  }

  // ---------- 再配達の担当ドライバーの割り振り（reassignDriver） ----------
  @Test
  void 割り振り_条件を満たせば配送物と再配送物を更新する() {
    // Given：Serviceが最後まで進めるように、Mapperが返すデータを用意する

    // 再配達10番を作る。中身のdeliveryIdは5番（＝配送物5番の再配達）
    // 再配達のidと配送物のidをわざと違う数字にして、取り違えたら気づけるようにする
    Redelivery redelivery = new Redelivery();
    redelivery.setId(10L);
    redelivery.setDeliveryId(5L);
    redelivery.setAllocationStatus("UNASSIGNED"); // 未割り当てにして、④の409チェックを通過させる
    redelivery.setRedeliveryDate(LocalDate.of(2026, 11, 10)); // now()は実行日で変わるので、日付は固定する
    redelivery.setRedeliveryTime("午前中");

    // 配送物5番を作る。発送準備中にして、⑤の409チェックを通過させる
    Delivery delivery = new Delivery();
    delivery.setId(5L);
    delivery.setStatus("PREPARING");

    // when(モック.メソッド(引数)).thenReturn(返す値)：「この引数で呼ばれたら、この値を返して」と決めておく
    when(redeliveryMapper.findById(10L)).thenReturn(Optional.of(redelivery)); // ① 再配達10番は見つかる
    when(deliveryMapper.findById(5L)).thenReturn(Optional.of(delivery)); // ② 配送物5番は見つかる
    when(userMapper.existsDriverById(2L)).thenReturn(true); // ③ ドライバー2番は存在する

    // When：テストしたいメソッドを実行する。再配達10番に、ドライバー2番を割り振る
    redeliveryService.reassignDriver(10L, new AssignDriverRequest(2L));

    // Then：更新のメソッドが、正しい引数で呼ばれたかを確かめる
    // verify(モック).メソッド(引数)：「このメソッドが、この引数で1回呼ばれたか」を確かめる。違えばテスト失敗
    // ⑥ 配送物5番（10番ではない）に、ドライバー2番と、再配達の日付・時間帯がコピーされたか
    verify(deliveryMapper).updateDriverAndDateTimeById(5L, 2L, LocalDate.of(2026, 11, 10), "午前中");
    // ⑦ 再配達10番が割り当て済み（DONE）になったか
    verify(redeliveryMapper).updateAllocationStatusById(10L, "DONE");
  }

  @Test
  void 再配達が見つからない場合404を返す() {
    // Given：再配達10番が見つからない（①で止まるので、配送物などの準備はいらない）
    when(redeliveryMapper.findById(10L)).thenReturn(Optional.empty());

    // When/Then
    assertThatThrownBy(() -> redeliveryService.reassignDriver(10L, new AssignDriverRequest(2L)))
        .isInstanceOf(NotFoundException.class)
        .hasMessage("該当する再配達が見つかりません");
  }

  @Test
  void 配送物が見つからない場合404を返す() {
    // Given：再配達10番は見つかるが、中身の配送物5番が見つからない（②で止まる）
    Redelivery redelivery = new Redelivery();
    redelivery.setId(10L);
    redelivery.setDeliveryId(5L);

    when(redeliveryMapper.findById(10L)).thenReturn(Optional.of(redelivery));
    when(deliveryMapper.findById(5L)).thenReturn(Optional.empty());

    // When/Then
    assertThatThrownBy(() -> redeliveryService.reassignDriver(10L, new AssignDriverRequest(2L)))
        .isInstanceOf(NotFoundException.class)
        .hasMessage("該当する配送物が見つかりません");
  }

  @Test
  void ドライバーではない場合404を返す() {
    // Given：再配達と配送物は見つかるが、ドライバー2番が存在しない（③で止まる）
    Redelivery redelivery = new Redelivery();
    redelivery.setId(10L);
    redelivery.setDeliveryId(5L);

    Delivery delivery = new Delivery();
    delivery.setId(5L);

    when(redeliveryMapper.findById(10L)).thenReturn(Optional.of(redelivery));
    when(deliveryMapper.findById(5L)).thenReturn(Optional.of(delivery));
    when(userMapper.existsDriverById(2L)).thenReturn(false);

    // When/Then
    assertThatThrownBy(() -> redeliveryService.reassignDriver(10L, new AssignDriverRequest(2L)))
        .isInstanceOf(NotFoundException.class)
        .hasMessage("該当するドライバーが見つかりません");
  }

  @Test
  void 割り当て済みの場合409() {
    // Given：再配達10番がすでに割り当て済み（DONE）になっている（④で止まる）
    Redelivery redelivery = new Redelivery();
    redelivery.setId(10L);
    redelivery.setDeliveryId(5L);
    redelivery.setAllocationStatus("DONE");

    Delivery delivery = new Delivery();
    delivery.setId(5L);
    delivery.setStatus("PREPARING");

    when(redeliveryMapper.findById(10L)).thenReturn(Optional.of(redelivery));
    when(deliveryMapper.findById(5L)).thenReturn(Optional.of(delivery));
    when(userMapper.existsDriverById(2L)).thenReturn(true);

    // When/Then
    assertThatThrownBy(() -> redeliveryService.reassignDriver(10L, new AssignDriverRequest(2L)))
        .isInstanceOf(ConflictException.class)
        .hasMessage("未割り当ての再配達のみ担当を割り振れます");
  }

  @Test
  void 発送準備中ではない場合409を返す() {
    // Given：再配達は未割り当てだが、配送物5番が配送中（IN TRANSIT）になっている（⑤で止まる）
    Redelivery redelivery = new Redelivery();
    redelivery.setId(10L);
    redelivery.setDeliveryId(5L);
    redelivery.setAllocationStatus("UNASSIGNED");

    Delivery delivery = new Delivery();
    delivery.setId(5L);
    delivery.setStatus("IN TRANSIT");

    when(redeliveryMapper.findById(10L)).thenReturn(Optional.of(redelivery));
    when(deliveryMapper.findById(5L)).thenReturn(Optional.of(delivery));
    when(userMapper.existsDriverById(2L)).thenReturn(true);

    // When/Then
    assertThatThrownBy(() -> redeliveryService.reassignDriver(10L, new AssignDriverRequest(2L)))
        .isInstanceOf(ConflictException.class)
        .hasMessage("発送準備中の配送物のみ担当を割り振れます");
  }
}
