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
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Objects.nonNull;

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
            metadata.set("meta:path", fileUpload.getFilename());
            String docType = extractDocType(metadata);
            String filename = metadata.get("meta:path");
            String thumbnail = "pdf.jpg"; // TODO extract thumbnail from first page
            doc = new SearchableBinary();
            doc.setDocType(docType);
            doc.setBody(content);
            doc.setThumbnail(thumbnail);
            doc.setRegisteredAt(new Date());
            doc.setReference(filename);
            doc.setId(SearchableBinary.PREFIX_TYPE.concat(":").concat(fileUpload.getId()));
            doc.setMetadata(converter.from(metadata));
            /*
            doc = SearchableBinary.builder().docType(docType)
                    .metadata(converter.from(metadata))
                    .body(content)
                    .thumbnail(thumbnail)
                    .registeredAt(new Date())
                    .reference(filename)
                    .id(documentId)
                    .build();
             */
            LOGGER.info("metadata: {}",doc.getMetadata());
            return doc;
        } catch (TikaException | IOException ex) {
            LOGGER.error("{} analyzing input stream",ex.getClass().getSimpleName(), ex);
            //TODO handle exception
            throw new RuntimeException(ex);
        }
    }

    private String extractDocType(Metadata metadata) {
		/*date=2020-01-20T10:53:00Z
	    cp:revision=2
	    extended-properties:AppVersion=16.0000
	    meta:paragraph-count=28
	    meta:word-count=2165
	    extended-properties:Company= Word-Count=2165
	    dcterms:created=2020-01-20T10:53:00Z meta:line-count=102
	    dcterms:modified=2020-01-20T10:53:00Z Last-Modified=2020-01-20T10:53:00Z
	    Last-Save-Date=2020-01-20T10:53:00Z meta:character-count=12342
	    Template=Normal.dotm Line-Count=102 Paragraph-Count=28
	    meta:save-date=2020-01-20T10:53:00Z meta:character-count-with-spaces=14479
		Application-Name=Microsoft Office Word modified=2020-01-20T10:53:00Z Content-Type=application/vnd.openxmlformats-officedocument.wordprocessingml.document
	    X-Parsed-By=org.apache.tika.parser.DefaultParser
	    X-Parsed-By=org.apache.tika.parser.microsoft.ooxml.OOXMLParser
	    meta:creation-date=2020-01-20T10:53:00Z
	    extended-properties:Application=Microsoft Office Word meta:last-author=Jose Molina Gonzalez
		Creation-Date=2020-01-20T10:53:00Z xmpTPg:NPages=14 Character-Count-With-Spaces=14479
	    Last-Author=Jose Molina Gonzalez Character Count=12342 Page-Count=14 Revision-Number=2 Application-Version=16.0000
	   extended-properties:Template=Normal.dotm publisher= meta:page-count=14 dc:publisher=
        */
		/*
		metadata: pdf:unmappedUnicodeCharsPerPage=0 pdf:unmappedUnicodeCharsPerPage=0
		pdf:unmappedUnicodeCharsPerPage=0 pdf:unmappedUnicodeCharsPerPage=0 pdf:unmappedUnicodeCharsPerPage=0
		pdf:PDFVersion=1.3 pdf:docinfo:title=PDF xmp:CreatorTool=Pdf995 Keywords=pdf, create pdf, software, acrobat, adobe
		pdf:hasXFA=false access_permission:modify_annotations=true access_permission:can_print_degraded=true
		subject=Create PDF with Pdf 995 dc:creator=Software 995
		dcterms:created=2003-12-12T17:30:12Z dc:format=application/pdf; version=1.3
		title=PDF
		pdf:docinfo:creator_tool=Pdf995 access_permission:fill_in_form=true
		pdf:docinfo:keywords=pdf, create pdf, software, acrobat, adobe pdf:encrypted=false
		dc:title=PDF cp:subject=Create PDF with Pdf 995 pdf:docinfo:subject=Create PDF with Pdf 995
		Content-Type=application/pdf
		pdf:docinfo:creator=Software 995 X-Parsed-By=org.apache.tika.parser.DefaultParser
		X-Parsed-By=org.apache.tika.parser.pdf.PDFParser
		creator=Software 995 meta:author=Software 995 meta:creation-date=2003-12-12T17:30:12Z access_permission:extract_for_accessibility=true
		access_permission:assemble_document=true xmpTPg:NPages=5 Creation-Date=2003-12-12T17:30:12Z
		pdf:hasXMP=false pdf:charsPerPage=2993 pdf:charsPerPage=2087 pdf:charsPerPage=2269 pdf:charsPerPage=1383
		pdf:charsPerPage=719
		access_permission:extract_content=true
		access_permission:can_print=true meta:keyword=pdf, create pdf, software, acrobat, adobe
		Author=Software 995 access_permission:can_modify=true
		pdf:docinfo:producer=GNU Ghostscript 7.05
		pdf:docinfo:created=2003-12-12T17:30:12Z
		* */
        String format = metadata.get(TikaCoreProperties.FORMAT);
        return Objects.isNull(format) ? "unknown" : metadata.get(TikaCoreProperties.FORMAT).split(";")[0];

    }

    static class MetadataConverter {
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
            System.out.println(metadata.toString());
           /*
           pdf:unmappedUnicodeCharsPerPage=0 pdf:unmappedUnicodeCharsPerPage=0 pdf:unmappedUnicodeCharsPerPage=0
           pdf:unmappedUnicodeCharsPerPage=0 pdf:unmappedUnicodeCharsPerPage=0
           pdf:PDFVersion=1.3 meta:path=sample2 (1).pdf
           pdf:docinfo:title=PDF xmp:CreatorTool=Pdf995
           Keywords=pdf, create pdf, software, acrobat, adobe
           pdf:hasXFA=false access_permission:modify_annotations=true
           access_permission:can_print_degraded=true
           subject=Create PDF with Pdf 995 dc:creator=Software 995
           dcterms:created=2003-12-12T17:30:12Z
           dc:format=application/pdf; version=1.3 title=PDF
           pdf:docinfo:creator_tool=Pdf995
           access_permission:fill_in_form=true
           pdf:docinfo:keywords=pdf, create pdf, software, acrobat, adobe
           pdf:encrypted=false dc:title=PDF cp:subject=Create PDF with Pdf 995
           pdf:docinfo:subject=Create PDF with Pdf 995 Content-Type=application/pdf
           pdf:docinfo:creator=Software 995
           X-Parsed-By=org.apache.tika.parser.DefaultParser X-Parsed-By=org.apache.tika.parser.pdf.PDFParser
           creator=Software 995 meta:author=Software 995
           meta:creation-date=2003-12-12T17:30:12Z
           access_permission:extract_for_accessibility=true
           access_permission:assemble_document=true xmpTPg:NPages=5 Creation-Date=2003-12-12T17:30:12Z pdf:hasXMP=false
           pdf:charsPerPage=2993 pdf:charsPerPage=2087 pdf:charsPerPage=2269 pdf:charsPerPage=1383
           pdf:charsPerPage=719 access_permission:extract_content=true access_permission:can_print=true
           meta:keyword=pdf, create pdf, software, acrobat, adobe Author=Software 995 access_permission:can_modify=true
           pdf:docinfo:producer=GNU Ghostscript 7.05
           pdf:docinfo:created=2003-12-12T17:30:12Z
*/
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
            /*
            SearchableBinary.Metadata meta = SearchableBinary.Metadata.builder()
                    .author(extractValue(PROPERTYNAME_AUTHOR, metadata))
                    .createdAt(extractValue(PROPERTYNAME_CREATED_AT,metadata))
                    .lastUpdatedAt(extractValue(PROPERTYNAME_UPDATED_AT,metadata)) //TODO PARSE dates multi-formats
                    .lastUpdatedBy(extractValue(PROPERTYNAME_UPDATED_BY,metadata))
                    .build();
             */
            meta.getKeywords().addAll(List.of(extractArrayValue(PROPERTYNAME_KEYWORDS,metadata)));
            return meta;
        }
    }
}
