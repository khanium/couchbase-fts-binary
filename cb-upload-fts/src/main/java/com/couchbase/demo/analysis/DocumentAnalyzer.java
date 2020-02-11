package com.couchbase.demo.analysis;

import com.couchbase.demo.binaries.SearchableBinary;
import com.couchbase.demo.upload.FileUpload;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Date;
import java.util.Objects;

@Component
public class DocumentAnalyzer {
    private final Logger LOGGER = LoggerFactory.getLogger(DocumentAnalyzer.class);

    private MetadataConverter converter = new MetadataConverter();
    private final Tika analyzer;

    @Autowired
    public DocumentAnalyzer(Tika analyzer) {
        this.analyzer = analyzer;
    }

    public SearchableBinary analyze(FileUpload fileUpload) {
        return analyze(new Metadata(), fileUpload);
    }

    public SearchableBinary analyze(Metadata metadata, FileUpload fileUpload) {
        SearchableBinary doc;
        try {
            String content = analyzer.parseToString(fileUpload.getSearchContentStream(), metadata);

            doc = SearchableBinary.builder()
                    .id(SearchableBinary.PREFIX_TYPE.concat(":").concat(fileUpload.getId()))
                    .docType(extractDocType(metadata))
                    .metadata(converter.from(metadata))
                    .body(content)
                    .thumbnail("pdf.jpg") // TODO extract thumbnail from first page
                    .registeredAt(new Date())
                    .reference(fileUpload.getFilename())
                    .build();

            return doc;
        } catch (TikaException | IOException ex) {
            LOGGER.error("{} analyzing input stream",ex.getClass().getSimpleName(), ex);
            //TODO handle exception
            throw new RuntimeException(ex);
        }
    }

    private String extractDocType(Metadata metadata) {
        String format = metadata.get(TikaCoreProperties.FORMAT);
        return Objects.isNull(format) ? "unknown" : metadata.get(TikaCoreProperties.FORMAT).split(";")[0];

    }

}
