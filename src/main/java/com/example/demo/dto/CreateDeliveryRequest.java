package com.example.demo.dto;

import com.example.demo.entity.Delivery;//toEntity()でDelivery（Entity）を作るので、そのクラスを使えるようにしている
import jakarta.validation.constraints.Email;//入力チェック用のアノテーション
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;//配達日を日付の型で受けとるために使用。

public record CreateDeliveryRequest(// recordは「値を入れて運ぶだけの入れ物」を短く書くための書き方。
    // @NotBlank：null、空文字 ""、空白だけ " " を弾きます。
    // message：引っかかったときに details に出る文言です。
    // String name：JSON の "name" の値がここに入ります。JSON のキー名と項目名を一致させる必要があります。
    //
    @NotBlank(message = "お届け先の氏名を入力してください") @Size(max = 50, message = "氏名は50文字以内で入力してください") String name,

    @NotBlank(message = "住所を入力してください") @Size(max = 100, message = "住所は100文字以内で入力してください") String address,

    @NotBlank(message = "電話番号を入力してください") @Pattern(regexp = "^[0-9]{10,11}$", message = "電話番号はハイフンなしの10桁または11桁の数字で入力してください") String callNumber,
    // @Pattern(regexp = ...)：正規表現（文字の並びのルール）に合わない値を弾きます。

    @NotBlank(message = "メールアドレスを入力してください") @Email(message = "メールアドレスの形式が正しくありません") @Size(max = 100, message = "メールアドレスは100文字以内で入力してください") String email,
    // @Email：@ があるかなど、メールアドレスの形になっているかを確認します。
    @NotBlank(message = "荷物の種類を入力してください") @Pattern(regexp = "NORMAL|RAW FOOD|FROZEN|FRAGILE", message = "荷物の種類の指定が正しくありません") String itemType,

    @NotNull(message = "配達日を入力してください") LocalDate deliveryDate,
    // LocalDate：JSON の "2026-10-28" が、日付の型に自動で変換されます。形式が違うと、既存の handleNotReadable で
    // 400 になります。
    // @NotNull：LocalDate は文字列ではないので、@NotBlank は使えません。「値がない」ことだけを確認する @NotNull
    // を使います。
    @NotBlank(message = "時間帯を入力してください") @Pattern(regexp = "午前中|12:00-14:00|14:00-16:00|16:00-18:00|18:00-20:00", message = "時間帯の指定が正しくありません") String deliveryTime) {
  public Delivery toEntity() {// このリクエストの内容を、DB 登録用の Delivery（Entity）に詰め替えるメソッドです。Service から
                              // request.toEntity() で呼びます。
    Delivery delivery = new Delivery();
    delivery.setName(name);
    delivery.setAddress(address);
    delivery.setCallNumber(callNumber);
    delivery.setEmail(email);
    delivery.setItemType(itemType);
    delivery.setDeliveryDate(deliveryDate);
    delivery.setDeliveryTime(deliveryTime);
    return delivery;
  }
}
