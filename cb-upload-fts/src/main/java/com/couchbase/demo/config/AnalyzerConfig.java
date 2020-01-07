package com.couchbase.demo.config;

import org.apache.tika.Tika;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AnalyzerConfig {

    @Bean
    public Tika analyzer() {
        //TODO set Tika Config parameters here
        return new Tika();
    }
}
