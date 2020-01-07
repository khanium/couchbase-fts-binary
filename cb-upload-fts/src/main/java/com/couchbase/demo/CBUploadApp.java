package com.couchbase.demo;

import com.couchbase.demo.storage.StorageProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(StorageProperties.class)
public class CBUploadApp {
    public static void main(String[] args) {
        SpringApplication.run(CBUploadApp.class, args);
    }
}
