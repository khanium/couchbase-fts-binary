package com.couchbase.demo.storage;

import com.couchbase.demo.upload.FileUpload;
import org.springframework.core.io.Resource;

import java.nio.file.Path;
import java.util.stream.Stream;

public interface StorageService {

	void init();

	void store(FileUpload file);

	Stream<Path> loadAll();

	Path load(String filename);

	Resource loadAsResource(String filename);

	void deleteAll();

	//SearchResult binarySearch(String content);

}
