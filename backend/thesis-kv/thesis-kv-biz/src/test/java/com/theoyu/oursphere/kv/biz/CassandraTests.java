package com.theoyu.thesis.kv.biz;

import com.theoyu.framework.common.utils.JsonUtils;
import com.theoyu.thesis.kv.biz.model.entity.ExampleContentPO;
import com.theoyu.thesis.kv.biz.model.repository.ExampleContentRepository;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Optional;
import java.util.UUID;

@SpringBootTest
@Slf4j
public class CassandraTests {


    @Resource
    private ExampleContentRepository noteContentRepository;

    /**
     * 测试插入数据
     */
    @Test
    void testInsert() {
        ExampleContentPO nodeContent = ExampleContentPO.builder()
                .id(UUID.randomUUID())
                .content("代码测试笔记内容插入")
                .build();

        noteContentRepository.save(nodeContent);
    }
    /**
     * 测试修改数据
     */
    @Test
    void testUpdate() {
        ExampleContentPO nodeContent = ExampleContentPO.builder()
                .id(UUID.fromString("eaad1222-f091-40be-b824-0c9f275724a7"))
                .content("代码测试笔记内容更新")
                .build();

        noteContentRepository.save(nodeContent);
    }

    /**
     * 测试查询数据
     */
    @Test
    void testSelect() {
        Optional<ExampleContentPO> optional = noteContentRepository.findById(UUID.fromString("eaad1222-f091-40be-b824-0c9f275724a7"));
        optional.ifPresent(noteContentDO -> log.info("查询结果：{}", JsonUtils.toJsonString(noteContentDO)));
    }

    /**
     * 测试删除数据
     */
    @Test
    void testDelete() {
        noteContentRepository.deleteById(UUID.fromString("eaad1222-f091-40be-b824-0c9f275724a7"));
    }
}
