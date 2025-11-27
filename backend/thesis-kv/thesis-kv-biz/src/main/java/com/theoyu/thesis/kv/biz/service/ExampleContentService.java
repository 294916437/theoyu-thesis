package com.theoyu.thesis.kv.biz.service;

import com.theoyu.framework.common.response.Response;
import com.theoyu.thesis.kv.dto.request.AddExampleContentReqDTO;
import com.theoyu.thesis.kv.dto.request.DeleteExampleContentReqDTO;
import com.theoyu.thesis.kv.dto.request.FindExampleContentReqDTO;
import com.theoyu.thesis.kv.dto.response.FindExampleContentRspDTO;

public interface ExampleContentService {
    /**
     * 添加笔记内容
     *
     * @param addExampleContentReqDTO
     * @return
     */
    Response<?> addExampleContent(AddExampleContentReqDTO addExampleContentReqDTO);
    /**
     * 查询笔记内容
     *
     * @param findExampleContentReqDTO
     * @return
     */
    Response<FindExampleContentRspDTO> findExampleContent(FindExampleContentReqDTO findExampleContentReqDTO);
    /**
     * 删除笔记内容
     *
     * @param deleteExampleContentReqDTO
     * @return
     */
    Response<?> deleteExampleContent(DeleteExampleContentReqDTO deleteExampleContentReqDTO);

}
