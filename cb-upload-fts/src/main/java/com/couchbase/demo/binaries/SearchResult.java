package com.couchbase.demo.binaries;

import com.couchbase.client.java.search.result.SearchQueryResult;
import com.couchbase.client.java.search.result.SearchQueryRow;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.logging.log4j.util.Strings;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@NoArgsConstructor
@Data
public class SearchResult {
    private Long total;
    private final List<SearchHit> hits = new ArrayList<>();

    public static SearchResult from(SearchQueryResult result) {
        SearchResult val = new SearchResult();
        val.setTotal(result.metrics().totalHits());
        val.getHits().addAll(result.hits().stream().map(SearchHit::from).collect(Collectors.toList()));
        return val;
    }

    @NoArgsConstructor
    @Data
    static class SearchHit {
        private String id;
        private Double score;
        private String docType;
        private String thumbnail;
        private String author;
        private String highlights;
        private Long registeredAt;
        private String createdAt;
        private String tags;
        private String title;
        private String reference;


        public static SearchHit from(SearchQueryRow item) {
            SearchHit hit = new SearchHit();
            hit.setId(item.id());
            hit.setScore(item.score());
            String highlights = item.fragments().containsKey("body") ?  Strings.join(item.fragments().get("body").iterator(),' ') : "";
            hit.setHighlights(highlights);
            hit.setRegisteredAt(Long.valueOf(item.fields().get("registeredAt")));
            hit.setDocType(item.fields().get("docType"));
            // TODO hit.setTitle(item.fields().get("title"));
            hit.setCreatedAt(item.fields().get("metadata.createdAt"));
            hit.setAuthor(item.fields().get("metadata.author"));
            hit.setTags(item.fields().get("metadata.keywords"));
            hit.setReference(item.fields().get("reference"));
            hit.setThumbnail(item.fields().get("thumbnail"));
            return hit;
        }
    }

}
