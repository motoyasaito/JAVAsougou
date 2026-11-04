package com.example.demo.service;

import java.time.LocalDate;
import java.time.ZoneId;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.demo.dto.RedeliveryResponse;
import com.example.demo.entity.Delivery;
import com.example.demo.entity.Redelivery;
import com.example.demo.dto.CreateRedeliveryRequest;
import com.example.demo.mapper.DeliveryMapper;
import com.example.demo.mapper.RedeliveryMapper;
import com.example.demo.mapper.UserMapper;
import com.example.demo.exception.NotFoundException;
import com.example.demo.exception.BadRequestException;
import com.example.demo.exception.ConflictException;
import com.example.demo.dto.AssignDriverRequest;

@Service
public class RedeliveryService {
  private final UserMapper userMapper;
  private final DeliveryMapper deliveryMapper;
  private final RedeliveryMapper redeliveryMapper;

  public RedeliveryService(DeliveryMapper deliveryMapper, RedeliveryMapper redeliveryMapper, UserMapper userMapper) {
    this.deliveryMapper = deliveryMapper;
    this.redeliveryMapper = redeliveryMapper;
    this.userMapper = userMapper;
  }

  // 再配達登録し、配送物のステータスをPREPARINGに戻す
  @Transactional
  public RedeliveryResponse create(CreateRedeliveryRequest request) {
    Delivery delivery = deliveryMapper.findByTrackingNumber(request.trackingNumber())
        .orElseThrow(() -> new NotFoundException("該当する配送物が見つかりません"));
    if (!delivery.getCallNumber().equals(request.callNumber())) {
      throw new NotFoundException("該当する配送物が見つかりません");
    }
    LocalDate today = LocalDate.now(ZoneId.of("Asia/Tokyo"));
    if (!request.redeliveryDate().isAfter(today)) {
      throw new BadRequestException("再配達日は明日以降の日付を選択してください");
    }
    if (!delivery.getStatus().equals("ABSENT")) {
      throw new ConflictException("不在の配送物のみ再配達を受け付けられます");
    }
    Redelivery redelivery = new Redelivery();
    redelivery.setDeliveryId(delivery.getId());
    redelivery.setRedeliveryDate(request.redeliveryDate());
    redelivery.setRedeliveryTime(request.redeliveryTime());
    redeliveryMapper.insert(redelivery);

    deliveryMapper.updateStatus(delivery.getId(), "PREPARING");
    return new RedeliveryResponse(delivery.getTrackingNumber(), request.redeliveryDate(), request.redeliveryTime());
  }

  // 再配達に担当ドライバーを割り振れる。未割当のものにだけ割り振る
  // @Transactional：このメソッドの中の更新を「ひとまとめ」にする。途中で失敗したら全部の更新を取り消す
  @Transactional
  // id：URLで指定された再配達のid / request：画面から送られてきたドライバーのid
  public void reassignDriver(long id, AssignDriverRequest request) {
    // ① 再配達をidで1件探す。見つからなければ404（NotFoundException）を投げて、ここで処理を終える
    Redelivery redelivery = redeliveryMapper.findById(id)
        .orElseThrow(() -> new NotFoundException("該当する再配達が見つかりません"));
    // ② 再配達が持っている「配送物のid（deliveryId）」を使って、元の配送物を探す。見つからなければ404
    Delivery delivery = deliveryMapper.findById(redelivery.getDeliveryId())
        .orElseThrow(() -> new NotFoundException("該当する配送物が見つかりません"));
    // ③ 送られてきたidのドライバーが存在するか確かめる。いなければ（falseなら）404
    if (!userMapper.existsDriverById(request.driverId())) {
      throw new NotFoundException("該当するドライバーが見つかりません");
    }
    // ④ すでに担当が割り振られている（DONE）再配達なら、二重に割り振らないよう409
    if (redelivery.getAllocationStatus().equals("DONE")) {
      throw new ConflictException("未割り当ての再配達のみ担当を割り振れます");
    }
    // ⑤ 配送物が「発送準備中（PREPARING）」でなければ409。配達中や配達完了の荷物には割り振れない
    if (!delivery.getStatus().equals("PREPARING")) {
      throw new ConflictException("発送準備中の配送物のみ担当を割り振れます");
    }
    // ⑥ 配送物の担当ドライバーと配達日時を更新する。日時は再配達で希望された日付・時間帯をコピーする
    deliveryMapper.updateDriverAndDateTimeById(delivery.getId(), request.driverId(), redelivery.getRedeliveryDate(),
        redelivery.getRedeliveryTime());
    // ⑦ 再配達を「割り当て済み（DONE）」にする。これで次に同じ再配達へ割り振ろうとすると④で止まる
    redeliveryMapper.updateAllocationStatusById(id, "DONE");
  }

}
