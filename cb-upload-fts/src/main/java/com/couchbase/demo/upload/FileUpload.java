package com.couchbase.demo.upload;

import com.couchbase.client.deps.io.netty.buffer.ByteBuf;
import com.couchbase.client.deps.io.netty.buffer.Unpooled;
import com.couchbase.demo.binaries.SearchableBinary;
import com.couchbase.demo.storage.StorageException;
import lombok.Builder;
import lombok.Value;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.Objects;

@Value
@Builder
public class FileUpload implements Closeable {
    String id;
    String filename;
    InputStream searchContentStream;
    InputStream storageFileStream;
    ByteBuf couchbaseBinaryByteBuf;

    public static FileUpload from(MultipartFile file) {
        String filename = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
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
            return FileUpload.builder()
                    .id(extractId(filename))
                    .filename(filename)
                    .searchContentStream(new BufferedInputStream(new ByteArrayInputStream(cache)))
                    .storageFileStream(new BufferedInputStream(new ByteArrayInputStream(cache))) //TODO remove storage file system OR couchbase binary storage
                    .couchbaseBinaryByteBuf(Unpooled.wrappedBuffer(cache))
                    .build();
        }
        catch (IOException e) {
            throw new StorageException("Failed to store file " + filename, e);
        }
        /*
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


    private static String extractId(String name) {
        return name.trim()
                .replace(" ","-")
                .replace("/","_")
                .replace("\\","_")
                .replace(".",":").toLowerCase();
    }

    @Override
    public void close() throws IOException {
        searchContentStream.close();
        storageFileStream.close();
        couchbaseBinaryByteBuf.clear();
    }
}
