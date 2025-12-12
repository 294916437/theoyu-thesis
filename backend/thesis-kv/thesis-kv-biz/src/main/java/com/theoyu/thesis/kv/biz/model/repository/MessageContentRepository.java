package com.theoyu.thesis.kv.biz.model.repository;

import com.theoyu.thesis.kv.biz.model.entity.MessageContentPO;
import org.springframework.data.cassandra.repository.CassandraRepository;

import java.util.UUID;


public interface MessageContentRepository extends CassandraRepository<MessageContentPO, UUID> {
}