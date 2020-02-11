package com.couchbase.demo.binaries;

import com.couchbase.client.java.repository.annotation.Field;
import com.couchbase.client.java.repository.annotation.Id;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.data.couchbase.core.mapping.Document;

import java.util.*;

@Document
@Data
@NoArgsConstructor
public class SearchableBinary {
    public static final String PREFIX_TYPE="searchable";

    @Id
    private String id;
    private String body;
    private String reference;
    private String docType;
    @Field
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Date registeredAt;
    private String thumbnail;
    @Field
    @JsonUnwrapped
    private Metadata metadata;

    @Data
    @NoArgsConstructor
    @ToString
    public static class Metadata {
        private String createdAt;
        private String lastUpdatedBy;
        private String lastUpdatedAt;
        private String author;
        private final List<String> keywords = new ArrayList<>();

        @JsonUnwrapped
        private final Map<String, String> others = new HashMap<>();

        @JsonUnwrapped
        public Map<String,String> getOthers() {
            return others;
        }
    }
}
