package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.services.s3.S3Client;

//このクラスは設定ですという印。
@Configuration
public class S3Config {

  @Bean
  public S3Client s3Client() { //S3とやりとりするための道具。ファイルを送るなどのメソッドを持っている。
    return S3Client.create();
  }
}
