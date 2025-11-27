package com.theoyu.thesis.kv.biz.model.repository;

import com.theoyu.thesis.kv.biz.model.entity.ExampleContentPO;
import org.springframework.data.cassandra.repository.CassandraRepository;

import java.util.UUID;


public interface ExampleContentRepository extends CassandraRepository<ExampleContentPO, UUID>{


}
