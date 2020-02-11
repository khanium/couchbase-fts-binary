package com.couchbase.demo.analysis;

import com.couchbase.demo.binaries.SearchableBinary;
import org.apache.tika.metadata.Metadata;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Objects.nonNull;

public class MetadataConverter {
    private static final String PROPERTYNAME_AUTHOR = "author";
    private static final String PROPERTYNAME_CREATED_AT = "createdAt";
    private static final String PROPERTYNAME_UPDATED_AT = "lastUpdatedAt";
    private static final String PROPERTYNAME_UPDATED_BY = "lastUpdatedBy";
    private static final String PROPERTYNAME_KEYWORDS = "keywords";

    private static final List<String> RESERVED_WORDS = List.of(PROPERTYNAME_AUTHOR,PROPERTYNAME_CREATED_AT,PROPERTYNAME_UPDATED_AT,PROPERTYNAME_UPDATED_BY,PROPERTYNAME_KEYWORDS);
    private static final Map<String, List<String>> STANDARD_METADATA = new HashMap<>();      // TODO initialize the common attributes

    static {
        STANDARD_METADATA.put(PROPERTYNAME_AUTHOR, List.of("creator","meta:author","pdf:docinfo:creator","dc:creator","Last-Author", "pdf:docinfo:producer"));
        STANDARD_METADATA.put(PROPERTYNAME_CREATED_AT, List.of("Creation-Date","meta:creation-date","dcterms:created"));
        STANDARD_METADATA.put(PROPERTYNAME_UPDATED_AT, List.of("dcterms:modified","Last-Modified","Last-Save-Date","modified"));
        STANDARD_METADATA.put(PROPERTYNAME_UPDATED_BY, List.of("Last-Author","meta:last-author"));
        STANDARD_METADATA.put(PROPERTYNAME_KEYWORDS, List.of("Keywords","meta:keyword"));
    }

    public SearchableBinary.Metadata from(final org.apache.tika.metadata.Metadata metadata) {
        //TODO checkNonNull(metadata);
        //TODO checkNonNull(docType);
        SearchableBinary.Metadata meta = extractCommonMetadata(metadata);

        List<String> propertyNames = Stream.of(metadata.names()).collect(Collectors.toList());
        propertyNames.removeAll(RESERVED_WORDS);
        for(String name: propertyNames) {
            meta.getOthers().put(name, metadata.get(name)); // TODO improve metadata types storage (i.e. data, boolean, int, array ...etc)
        }

        return meta;
    }


    private String[] extractArrayValue(String key, Metadata metadata) {
        List<String> candidates = STANDARD_METADATA.get(key);
        String []empty = new String[]{};
        return nonNull(candidates) && !candidates.isEmpty() ?
                candidates.stream().map(metadata::getValues).filter(Objects::nonNull)
                        .findFirst().orElse(empty) : empty;
    }

    private Date extractDateValue(String key, Metadata metadata) {
        List<String> candidates = STANDARD_METADATA.get(key);
        return nonNull(candidates) && !candidates.isEmpty() ?
                candidates.stream().map(metadata::get).filter(Objects::nonNull) //TODO parse Date multi-formats
                        .map(LocalDateTime::parse).map(d -> Date.from(d.atZone(ZoneId.systemDefault()).toInstant()))
                        .findFirst().orElse(null) : null;
    }

    private String extractValue(String key, Metadata metadata) {
        List<String> candidates = STANDARD_METADATA.get(key);
        return nonNull(candidates) && !candidates.isEmpty() ?
                candidates.stream().map(metadata::get).filter(Objects::nonNull).findFirst().orElse(null) : null;
    }

    private SearchableBinary.Metadata extractCommonMetadata(Metadata metadata) {
        SearchableBinary.Metadata meta = new SearchableBinary.Metadata();
        meta.setAuthor(extractValue(PROPERTYNAME_AUTHOR, metadata));
        meta.setCreatedAt(extractValue(PROPERTYNAME_CREATED_AT,metadata));
        meta.setLastUpdatedAt(extractValue(PROPERTYNAME_UPDATED_AT,metadata)); //TODO PARSE dates multi-formats
        meta.setLastUpdatedBy(extractValue(PROPERTYNAME_UPDATED_BY,metadata));
        meta.getKeywords().addAll(List.of(extractArrayValue(PROPERTYNAME_KEYWORDS,metadata)));
        return meta;
    }
}
