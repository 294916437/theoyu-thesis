package com.theoyu.thesis.kv.biz.service.impl;

import com.theoyu.framework.common.exception.BusinessException;
import com.theoyu.framework.common.response.Response;
import com.theoyu.thesis.kv.biz.enums.ResponseCodeEnum;
import com.theoyu.thesis.kv.biz.model.entity.ExampleContentPO;
import com.theoyu.thesis.kv.biz.model.repository.ExampleContentRepository;
import com.theoyu.thesis.kv.biz.service.ExampleContentService;
import com.theoyu.thesis.kv.dto.request.AddExampleContentReqDTO;
import com.theoyu.thesis.kv.dto.request.DeleteExampleContentReqDTO;
import com.theoyu.thesis.kv.dto.request.FindExampleContentReqDTO;
import com.theoyu.thesis.kv.dto.response.FindExampleContentRspDTO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class ExampleContentServiceImpl implements ExampleContentService {
    @Resource
    private ExampleContentRepository exampleContentRepository;
    @Override
    public Response<?> addExampleContent(AddExampleContentReqDTO addExampleContentReqDTO) {
        // 笔记 ID
        String uuid = addExampleContentReqDTO.getUuid();
        // 笔记内容
        String content = addExampleContentReqDTO.getContent();

        // 构建数据库实体类
        ExampleContentPO nodeContent = ExampleContentPO.builder()
                .id(UUID.fromString(uuid))
                .content(content)
                .build();

        // 插入数据
        exampleContentRepository.save(nodeContent);

        return Response.success();
    }
    /**
     * 查询笔记内容
     **/
    @Override
    public Response<FindExampleContentRspDTO> findExampleContent(FindExampleContentReqDTO findExampleContentReqDTO) {
        // 笔记 ID
        String uuid = findExampleContentReqDTO.getUuid();
        // 根据笔记 ID 查询笔记内容
        Optional<ExampleContentPO> optional = exampleContentRepository.findById(UUID.fromString(uuid));

        // 若笔记内容不存在
        if (optional.isEmpty()) {
            throw new BusinessException(ResponseCodeEnum.EXAMPLE_CONTENT_NOT_FOUND);
        }

        ExampleContentPO exampleContentPO = optional.get();
        // 构建返参 DTO
        FindExampleContentRspDTO findExampleContentRspDTO = FindExampleContentRspDTO.builder()
                .uuid(exampleContentPO.getId())
                .content(exampleContentPO.getContent())
                .build();

        return Response.success(findExampleContentRspDTO);
    }

    @Override
    public Response<?> deleteExampleContent(DeleteExampleContentReqDTO deleteExampleContentReqDTO) {
        // 笔记 ID
        String uuid = deleteExampleContentReqDTO.getUuid();
        // 删除笔记内容
        exampleContentRepository.deleteById(UUID.fromString(uuid));

        return Response.success();
    }

}
