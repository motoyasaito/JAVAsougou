package com.example.demo.config;

import com.example.demo.dto.DeliveryRules;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// 画面（JSP）で共通に使う値の設定
@Configuration
public class ViewConfig {

  // アプリの起動時に、お客さん用の会社・サービスの名前を「アプリ全体で共有する場所」（ServletContext）に入れておく
  // ここに入れた値は、どのJSPからでも ${customerBrandName} で読める（Controller で毎回 Model に入れなくてよい）
  // エラー画面のように、Controller を通らずに表示される画面でも使えるようにするため
  @Bean
  public ServletContextInitializer customerBrandNameInitializer(
      @Value("${app.customer-brand-name}") String customerBrandName) {// @Value：application.properties の値を受け取る
    return servletContext -> servletContext.setAttribute("customerBrandName", customerBrandName);
  }

  // 配達の時間帯の選択肢も、同じように全画面で ${deliveryTimes} として使えるようにする
  // プルダウンの中身を JSP に直接書かず、入力チェックと同じ DeliveryRules から取るので、ずれない
  @Bean
  public ServletContextInitializer deliveryTimesInitializer() {
    return servletContext -> servletContext.setAttribute("deliveryTimes", DeliveryRules.DELIVERY_TIMES);
  }
}
