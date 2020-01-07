package com.couchbase.demo.analysis;

import lombok.Builder;
import lombok.Data;
import org.apache.tika.metadata.Metadata;

@Data
@Builder
public class FileDocument {
    private Metadata metadata;
    private String content;
    private String filename;
    private String docType;
}
