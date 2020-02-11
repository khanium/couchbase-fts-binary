package com.couchbase.demo.storage;

import com.couchbase.client.java.Bucket;
import com.couchbase.demo.upload.FileUpload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.stream.Stream;

import static java.time.LocalDateTime.now;

@Service
public class FileSystemStorageService implements StorageService {
	private final Logger LOGGER = LoggerFactory.getLogger(FileSystemStorageService.class);

	private final Path rootLocation;
	private final Bucket bucket;

	@Autowired
	public FileSystemStorageService(StorageProperties properties, Bucket bucket) {
		this.rootLocation = Paths.get(properties.getLocation());
		this.bucket = bucket;
	}

	@Override
	public void store(FileUpload file) {
		try {
			store(file.getFilename(), file.getStorageFileStream());
		} catch (IOException e) {
				throw new StorageException("Failed to store file " + file.getFilename(), e);
		}
	/*	LOGGER.info("storing...");
		String filename = StringUtils.cleanPath(file.getOriginalFilename());
		try {
			if (file.isEmpty()) {
				throw new StorageException("Failed to store empty file " + filename);
			}
			if (filename.contains("..")) {
				// This is a security check
				throw new StorageException("Cannot store file with relative path outside current directory "
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

	 */
	}

	private void store(String filename, InputStream inputStream) throws IOException{
		Files.copy(inputStream, this.rootLocation.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
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
