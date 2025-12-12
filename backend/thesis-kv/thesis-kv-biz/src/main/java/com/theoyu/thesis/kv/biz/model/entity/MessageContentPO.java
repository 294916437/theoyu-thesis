package com.theoyu.thesis.kv.biz.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

import java.util.UUID;

@Table("message_content")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageContentPO {
    @PrimaryKey("id")
    private UUID id;

    private String content;
}
