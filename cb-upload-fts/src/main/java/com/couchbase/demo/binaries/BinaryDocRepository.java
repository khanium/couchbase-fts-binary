package com.couchbase.demo.binaries;

import org.springframework.data.couchbase.repository.CouchbaseRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BinaryDocRepository extends CouchbaseRepository<BinaryDoc, String> {

}
