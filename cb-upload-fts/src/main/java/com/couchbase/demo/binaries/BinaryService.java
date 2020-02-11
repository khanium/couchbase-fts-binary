package com.couchbase.demo.binaries;

import com.couchbase.client.java.document.BinaryDocument;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.search.SearchQuery;
import com.couchbase.client.java.search.queries.QueryStringQuery;
import com.couchbase.client.java.search.result.SearchQueryResult;
import com.couchbase.demo.analysis.DocumentAnalyzer;
import com.couchbase.demo.upload.FileUpload;
import com.fasterxml.jackson.databind.util.RawValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;

@Service
public class BinaryService {
    private final Logger LOGGER = LoggerFactory.getLogger(BinaryService.class);
    public static final String []SEARCHING_FIELDS = {"registeredAt","reference","metadata", "metadata.author","metadata.createdAt","metadata.keywords","thumbnail"};

    private final SearchableBinaryRepository searchableBinaryRepository;
    private final BinaryDocRepository binaryDocRepository;
    private final DocumentAnalyzer analyzer;
    private final BinaryDocConverter adapter = new BinaryDocConverter();

    @Autowired
    public BinaryService(SearchableBinaryRepository searchableBinaryRepository, BinaryDocRepository binaryDocRepository, DocumentAnalyzer analyzer) {
        this.searchableBinaryRepository = searchableBinaryRepository;
        this.binaryDocRepository = binaryDocRepository;
        this.analyzer = analyzer;
    }

    public SearchableBinary findById(String docId) {
        return searchableBinaryRepository.findById(docId).orElse(null);
    }

    public SearchResult binarySearch(String content) {
        String indexName = "binarySearch";
        QueryStringQuery query = SearchQuery.queryString(content);

        SearchQueryResult result = searchableBinaryRepository.getCouchbaseOperations().getCouchbaseBucket()
                .query(new SearchQuery(indexName, query).limit(10).highlight().fields(SEARCHING_FIELDS));

        return SearchResult.from(result);
    }

    public SearchableBinary save(FileUpload file) {
        SearchableBinary searchableDoc = analyzer.analyze(file);
      //  save(adapter.convert(file)); // save binary as a blob in content property
        return save(searchableDoc);
    }


    private SearchableBinary save(SearchableBinary doc) {
        return searchableBinaryRepository.save(doc);
    }

    private BinaryDoc save(BinaryDoc doc) {
        return binaryDocRepository.save(doc);
    }


    private static class BinaryDocConverter {

        public BinaryDoc convert(FileUpload upload) {
            
            return BinaryDoc.builder()
                    .id(BinaryDoc.PREFIX_TYPE.concat(":".concat(upload.getId())))
                    .content(upload.getCouchbaseBinaryByteBuf()) // TODO save blob property
                    .channels(Arrays.asList("attachments"))
                    .build();
        }


    }
}
