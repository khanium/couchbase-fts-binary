package com.couchbase.demo.document;

import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;

@Component
public class DocumentAnalyzer {
    private final Logger LOGGER = LoggerFactory.getLogger(DocumentAnalyzer.class);
    private final Tika analyzer;

    @Autowired
    public DocumentAnalyzer(Tika analyzer) {
        this.analyzer = analyzer;
    }

    public FileDocument analyze(InputStream stream) {
        return analyze(new Metadata(), stream);
    }

    public FileDocument analyze(Metadata metadata, InputStream stream) {
        FileDocument doc = null;
        try {
            String content = analyzer.parseToString(stream, metadata);
            doc = FileDocument.builder().metadata(metadata).content(content).build();
        } catch (TikaException | IOException ex) {
            LOGGER.error("{} analyzing input stream",ex.getClass().getSimpleName(), ex);
            //TODO handle exception
            throw new RuntimeException(ex);
        }
        return doc;
    }

}
