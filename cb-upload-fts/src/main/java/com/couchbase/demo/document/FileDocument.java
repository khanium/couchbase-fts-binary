package com.couchbase.demo.document;

import lombok.Builder;
import lombok.Data;
import org.apache.tika.metadata.Metadata;

@Data
@Builder
public class FileDocument {
    private Metadata metadata;
    private String content;
}
