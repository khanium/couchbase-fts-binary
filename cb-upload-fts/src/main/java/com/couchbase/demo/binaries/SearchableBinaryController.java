package com.couchbase.demo.binaries;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("binaries")
public class SearchableBinaryController {
    private final Logger LOGGER = LoggerFactory.getLogger(SearchableBinaryController.class);
    private final BinaryService service;

    @Autowired
    public SearchableBinaryController(BinaryService service) {
        this.service = service;
    }

    @GetMapping("/{docId}")
    public ResponseEntity<SearchableBinary> get(@PathVariable("docId") String docId) {
        return ResponseEntity.ok(service.findById(docId));
    }

    @PostMapping("searching")
    public ResponseEntity<SearchResult> binarySearch(@RequestBody String content) {
        LOGGER.info("Searching... {}",content);
        return ResponseEntity.ok(service.binarySearch(content));
    }
}
