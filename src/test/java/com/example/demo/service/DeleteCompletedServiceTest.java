package com.example.demo.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.when;

import com.example.demo.mapper.DeliveryAlertMapper;
import com.example.demo.mapper.DeliveryMapper;
import com.example.demo.mapper.RedeliveryMapper;

// Mapper をモックにして、Service が「再配達 → 配送物」の順で、受け取った基準日時のまま削除を頼んでいるかを確認する
@ExtendWith(MockitoExtension.class) // Mockitoを使う宣言。これがないと@Mockや@InjectMocksが動かない
class DeleteCompletedServiceTest {

  @Mock
  RedeliveryMapper redeliveryMapper;// 再配達のマッパーの偽物。DBには行かない

  @Mock
  DeliveryMapper deliveryMapper;// 配送物のマッパーの偽物

  @Mock
  DeliveryAlertMapper deliveryAlertMapper; //アラートのマッパーの偽物

  @InjectMocks
  DeleteCompletedService deleteCompletedService;// 本物のServiceを作り、コンストラクタに上の偽物3つを渡す

  @Test
  void 削除_アラート再配達配送物の順で消す() {
    // Given テストで使う基準日時を決める
    LocalDateTime threshold = LocalDateTime.of(2026, 10, 1, 3, 0);// 2026/10/1/3:00

    // When 本物のServiceで削除を実行する。偽物のMapperは何も教えていないので、件数として0を返す
    deleteCompletedService.deleteCompleted(threshold);
    // Then アラート→再配達→配送物の順で、同じ基準日時で呼ばれたか確かめる
    InOrder inOrder = inOrder(deliveryAlertMapper, redeliveryMapper, deliveryMapper);// 順番を見張る係を作る
    inOrder.verify(deliveryAlertMapper).deleteByCompletedDeliveries(threshold);// 1番目に呼ばれたか
    inOrder.verify(redeliveryMapper).deleteByCompletedDeliveries(threshold);// 2番目に呼ばれたか
    inOrder.verify(deliveryMapper).deleteCompleted(threshold);// 3番目に呼ばれたか。順番が逆だとここで失敗する
  }

  @Test
  void 削除_配送物の削除で失敗したら例外をそのまま投げる() {
    // Given 配送物の削除でDBエラーが起きた状態を作る
    LocalDateTime threshold = LocalDateTime.of(2026, 10, 1, 3, 0);// 変数はテストごとに別なので、ここでも作る
    when(deliveryMapper.deleteCompleted(threshold)).thenThrow(new RuntimeException("DBエラー"));// この基準日時で呼ばれたら、DBエラーの代わりにRuntimeExceptionを投げると教える

    // When/Then Serviceが例外を握りつぶさず、そのまま外へ投げるか確かめる（外に出ることで@Transactionalが削除を取り消せる）
    // assertThatThrownByの中で呼んだ処理が例外を投げるか → isInstanceOfで種類を、hasMessageでメッセージを確かめる
    assertThatThrownBy(() -> deleteCompletedService.deleteCompleted(threshold)).isInstanceOf(RuntimeException.class)
        .hasMessage("DBエラー");
  }
}
