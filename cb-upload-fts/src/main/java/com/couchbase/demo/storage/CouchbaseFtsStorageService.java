package com.couchbase.demo.storage;

import com.couchbase.client.deps.io.netty.buffer.ByteBuf;
import com.couchbase.client.deps.io.netty.buffer.Unpooled;
import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.document.BinaryDocument;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.demo.document.DocumentAnalyzer;
import com.couchbase.demo.document.FileDocument;
import org.apache.tika.metadata.Metadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.Date;
import java.util.stream.Stream;

@Service
public class FileSystemStorageService implements StorageService {
	private final Logger LOGGER = LoggerFactory.getLogger(FileSystemStorageService.class);

	private final Path rootLocation;
	private final DocumentAnalyzer analyzer;
	private final Bucket bucket;

	@Autowired
	public FileSystemStorageService(StorageProperties properties, DocumentAnalyzer analyzer, Bucket bucket) {
		this.rootLocation = Paths.get(properties.getLocation());
		this.analyzer = analyzer;
		this.bucket = bucket;
	}

	@Override
	public void store(MultipartFile file) {
		LOGGER.info("storing...");
		String filename = StringUtils.cleanPath(file.getOriginalFilename());
		try {
			if (file.isEmpty()) {
				throw new StorageException("Failed to store empty file " + filename);
			}
			if (filename.contains("..")) {
				// This is a security check
				throw new StorageException(
						"Cannot store file with relative path outside current directory "
								+ filename);
			}
			byte []cache = StreamUtils.copyToByteArray(file.getInputStream()); // For reading inputstream twice
			ByteBuf byteBuf = Unpooled.wrappedBuffer(cache);
			try (
				 InputStream inputStream = new BufferedInputStream(new ByteArrayInputStream(cache));
				 InputStream inputStream2 = new BufferedInputStream(new ByteArrayInputStream(cache));) {
				storeInFileSystem(filename, inputStream);
				storeInCouchbase(filename, inputStream2, byteBuf);
			}

		}
		catch (IOException e) {
			throw new StorageException("Failed to store file " + filename, e);
		}
	}

	private void storeInFileSystem(String filename, InputStream inputStream) throws IOException{
		Files.copy(inputStream, this.rootLocation.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
	}

	private void storeInCouchbase(String filename, InputStream inputStream, ByteBuf byteBuf) throws IOException{
		FileDocument doc = analyzer.analyze(inputStream);
		print(doc); // TODO save in Couchbase
		String id = extractId(filename);
		bucket.upsert(JsonDocument.create(id, toJsonObject(doc)));
		bucket.upsert(BinaryDocument.create(id+"_attachment", byteBuf));
	}

	private JsonObject toJsonObject(FileDocument doc) {
		JsonObject content = JsonObject.empty();
		for(String property: doc.getMetadata().names()) {
			content = content.put(property, doc.getMetadata().isMultiValued(property) ?
					JsonArray.from(doc.getMetadata().getValues(property)) :
					doc.getMetadata().get(property));
		}
		content.put("body", doc.getContent());
		content.put("type","searchable");
		content.put("registeredAt", new Date());
		return content;
	}

	private String extractId(String name) {
		return name.trim()
				.replace(" ","-")
				.replace("/","_")
				.replace("\\","_")
				.replace(".","-").toLowerCase();
	}

	private void print(FileDocument doc) {
		LOGGER.info("metadata: {}",doc.getMetadata());
		LOGGER.info("content: {}", doc.getContent());
	}

	@Override
	public Stream<Path> loadAll() {
		try {
			return Files.walk(this.rootLocation, 1)
				.filter(path -> !path.equals(this.rootLocation))
				.map(this.rootLocation::relativize);
		}
		catch (IOException e) {
			throw new StorageException("Failed to read stored files", e);
		}

	}

	@Override
	public Path load(String filename) {
		return rootLocation.resolve(filename);
	}

	@Override
	public Resource loadAsResource(String filename) {
		try {
			Path file = load(filename);
			Resource resource = new UrlResource(file.toUri());
			if (resource.exists() || resource.isReadable()) {
				return resource;
			}
			else {
				throw new StorageFileNotFoundException(
						"Could not read file: " + filename);

			}
		}
		catch (MalformedURLException e) {
			throw new StorageFileNotFoundException("Could not read file: " + filename, e);
		}
	}

	@Override
	public void deleteAll() {
		FileSystemUtils.deleteRecursively(rootLocation.toFile());
	}

	@Override
	public void init() {
		try {
			Files.createDirectories(rootLocation);
		}
		catch (IOException e) {
			throw new StorageException("Could not initialize storage", e);
		}
	}
}
