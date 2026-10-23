package com.example.demo.config;

import io.swagger.v3.oas.models.OpenAPI; //springdocライブラリからOpenAPIクラスの取得
import io.swagger.v3.oas.models.info.Info;//springdocライブラリからInfoクラスの取得
import org.springframework.context.annotation.Configuration;//@Configurationのアノテーションを使えるようにしている
import org.springframework.context.annotation.Bean;//@Beanのアノテーションを使えるようにしている

@Configuration // 設定用のクラスだとspringに伝える
public class OpenApiConfig {

  @Bean // メソッドの返すオブジェクトをspringに管理させる。
  public OpenAPI openAPI() {
    return new OpenAPI()// OpenAPIのオブジェクトを新しく作り、それを返す。
        .info(new Info()// Infoのオブジェクトを新しく作り、OpenAPIに基本情報として設定。
            .title("配送管理API")// タイトルの設定
            .description("配送物・再配達・従業員が管理するAPI")// 説明の設定
            .version("v1"));// バージョンの設定
  }
}