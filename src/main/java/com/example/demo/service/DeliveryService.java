package com.example.demo.service;

import com.example.demo.dto.CreateDeliveryRequest;
import com.example.demo.dto.DeliveryResponse;
import com.example.demo.dto.PageResponse;
import com.example.demo.entity.Delivery;
import com.example.demo.exception.NotFoundException;
import com.example.demo.mapper.DeliveryMapper;
import com.example.demo.mapper.UserMapper;
import com.example.demo.dto.AssignDriverRequest;
import com.example.demo.exception.ConflictException;

import java.time.ZoneId;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeliveryService {

  private final DeliveryMapper deliveryMapper;
  private final TrackingNumberGenerator trackingNumberGenerator;// 伝票番号を作る道具。@Componentが付いているので、Springがコンストラクタに渡してくれる
  private final UserMapper userMapper;

  public DeliveryService(DeliveryMapper deliveryMapper, TrackingNumberGenerator trackingNumberGenerator,
      UserMapper userMapper) {
    this.deliveryMapper = deliveryMapper;
    this.trackingNumberGenerator = trackingNumberGenerator;
    this.userMapper = userMapper;
  }

  // 配送物の一覧を、page（0から始まるページ番号）とsize（1ページの件数）で取得する
  public PageResponse<DeliveryResponse> findAll(int page, int size) {
    long offset = (long) page * size; // 例: page=2, size=10 なら21件目から（20件飛ばす）

    List<DeliveryResponse> content = deliveryMapper.findAll(size, offset).stream()
        .map(DeliveryResponse::from)
        .toList();
    long total = deliveryMapper.count();

    return PageResponse.of(content, page, size, total);
  }

  public PageResponse<DeliveryResponse> findByDriver(long driverId, LocalDate deliveryDate, int page, int size) {
    LocalDate targetDate;
    if (deliveryDate == null) {
      targetDate = LocalDate.now(ZoneId.of("Asia/Tokyo"));
    } else {
      targetDate = deliveryDate;
    }
    long offset = (long) page * size;

    List<DeliveryResponse> content = deliveryMapper.findByDriverId(driverId, targetDate, size, offset).stream()
        .map(DeliveryResponse::from)
        .toList();
    long total = deliveryMapper.countByDriverId(driverId, targetDate);

    return PageResponse.of(content, page, size, total);
  }

  // 配送物を登録し、登録した内容を返す
  @Transactional // メソッド内のDB操作をひとまとまりにする。途中で例外が起きたら、登録も含めて全部取り消す
  public DeliveryResponse create(CreateDeliveryRequest request) {
    String trackingNumber = trackingNumberGenerator.generate();// 12桁の伝票番号を作る
    while (deliveryMapper.existsByTrackingNumber(trackingNumber)) {// すでに使われている番号なら、使われていない番号が出るまで作り直す
      trackingNumber = trackingNumberGenerator.generate();
    }
    Delivery delivery = request.toEntity();// 届いたリクエストを、DB登録用のDelivery（Entity）に詰め替える
    delivery.setTrackingNumber(trackingNumber);// 伝票番号はリクエストにないので、ここでセットする
    deliveryMapper.insert(delivery);// DBに登録する。登録後、DBが採番したidがdeliveryに入る（@Optionsの効果）
    // statusやcreated_atはDBが入れた値で、deliveryには入っていないので、idで取り直す
    // findByIdはOptional（中身がないかもしれない箱）を返す。orElseThrow()は中身があれば取り出し、なければ例外を投げる
    Delivery saved = deliveryMapper.findById(delivery.getId()).orElseThrow();
    return DeliveryResponse.from(saved);// EntityはそのままでなくDTOに詰め替えて返す
  }

  // 伝票番号で配送物を1件取得する。見つからなければ404にする
  public DeliveryResponse findByTrackingNumber(String trackingNumber) {
    Delivery delivery = deliveryMapper.findByTrackingNumber(trackingNumber)// 伝票番号でDBから探す。結果はOptional（中身がないかもしれない箱）で返ってくる
        .orElseThrow(() -> new NotFoundException("該当する配送物が見つかりません"));// 中身があれば取り出し、なければNotFoundExceptionを投げる（GlobalExceptionHandlerで404になる）
    return DeliveryResponse.from(delivery);// EntityをDTOに詰め替えて返す
  }

  // 配送物に担当ドライバーを割り振れる。発送準備中のものだけ割り振る
  @Transactional
  public void assignDriver(long id, AssignDriverRequest request) {
    Delivery delivery = deliveryMapper.findById(id)
        .orElseThrow(() -> new NotFoundException("該当する配送物が見つかりません"));
    if (!userMapper.existsDriverById(request.driverId())) {
      throw new NotFoundException("該当するドライバーが見つかりません");
    }
    if (!delivery.getStatus().equals("PREPARING")) {
      throw new ConflictException("発送準備中の配送物のみ担当を割り振れます");
    }
    deliveryMapper.updateDriverById(id, request.driverId());
  }
}
