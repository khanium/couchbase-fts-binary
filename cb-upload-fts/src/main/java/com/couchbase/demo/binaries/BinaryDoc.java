package com.couchbase.demo.binaries;

import com.couchbase.client.deps.io.netty.buffer.ByteBuf;
import com.couchbase.client.java.document.BinaryDocument;
import com.couchbase.client.java.repository.annotation.Id;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BinaryDoc {
    public static final String PREFIX_TYPE="binary";
    @Id
    String id;
    @Builder.Default
    private final String type = PREFIX_TYPE;
    @Builder.Default
    List<String> channels = new ArrayList<>();
    ByteBuf content; // TODO blob type

}
