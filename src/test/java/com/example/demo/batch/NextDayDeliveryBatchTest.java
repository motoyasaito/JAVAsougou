package com.example.demo.batch;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.time.LocalDate;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.demo.service.NextDayDeliveryService;

@ExtendWith(MockitoExtension.class)
public class NextDayDeliveryBatchTest {

  @Mock
  NextDayDeliveryService nextDayDeliveryService;

  @InjectMocks
  NextDayDeliveryBatch nextDayDeliveryBatch;

  @Test
  void 実行_基準日を今日にしてServiceを呼ぶ() {
    // Given
    LocalDate before = LocalDate.now(ZoneId.of("Asia/Tokyo"));

    // When
    nextDayDeliveryBatch.run();

    //Then
    LocalDate after = LocalDate.now(ZoneId.of("Asia/Tokyo"));
    ArgumentCaptor<LocalDate> captor = ArgumentCaptor.forClass(LocalDate.class);
    verify(nextDayDeliveryService).detectOverdueDeliveries(captor.capture());
    assertThat(captor.getValue()).isBetween(before, after);
  }

  @Test
  void 実行_検知に失敗しても例外を外に出さない() {
    // Given
    doThrow(new RuntimeException("DBエラー"))
        .when(nextDayDeliveryService).detectOverdueDeliveries(any());

    // When / Then
    assertThatCode(() -> nextDayDeliveryBatch.run())
        .doesNotThrowAnyException();
  }
}
