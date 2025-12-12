package com.theoyu.thesis.chat.biz.rpc;

import cn.hutool.core.collection.CollUtil;
import com.theoyu.framework.common.response.Response;
import com.theoyu.thesis.user.api.UserFeignApi;
import com.theoyu.thesis.user.dto.request.FindUserByIdReqDTO;
import com.theoyu.thesis.user.dto.request.FindUsersByIdsReqDTO;
import com.theoyu.thesis.user.dto.response.FindUserByIdRspDTO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class UserRpcService {
    @Resource
    private UserFeignApi userFeignApi;

    /**
     * 查询用户信息
     * @param userId
     * @return
     */
    public FindUserByIdRspDTO findById(Long userId) {
        FindUserByIdReqDTO findUserByIdReqDTO = new FindUserByIdReqDTO();
        findUserByIdReqDTO.setId(userId);

        Response<FindUserByIdRspDTO> response = userFeignApi.findById(findUserByIdReqDTO);

        if (Objects.isNull(response) || !response.isSuccess()) {
            return null;
        }

        return response.getData();
    }
    /**
     * 批量查询用户信息
     * @param userIds
     * @return
     */
    public List<FindUserByIdRspDTO> findByIds1(List<Long> userIds) {
        FindUsersByIdsReqDTO findUsersByIdsReqDTO = new FindUsersByIdsReqDTO();
        findUsersByIdsReqDTO.setIds(userIds);

        Response<List<FindUserByIdRspDTO>> response = userFeignApi.findByIds(findUsersByIdsReqDTO);

        if (Objects.isNull(response) || !response.isSuccess()) {
            return null;
        }

        return response.getData();
    }
    /**
     * 批量查询用户信息
     *
     * @param userIds 用户ID集合（Set、List 等）
     * @return 用户ID -> 用户信息的映射
     */
    public Map<Long, FindUserByIdRspDTO> findByIds2(Collection<Long> userIds) {
        if (CollUtil.isEmpty(userIds)) {
            return new HashMap<>();
        }

        FindUsersByIdsReqDTO findUsersByIdsReqDTO = new FindUsersByIdsReqDTO();
        findUsersByIdsReqDTO.setIds(new ArrayList<>(userIds));

        Response<List<FindUserByIdRspDTO>> response = userFeignApi.findByIds(findUsersByIdsReqDTO);

        if (Objects.isNull(response) || !response.isSuccess() || Objects.isNull(response.getData())) {
            return new HashMap<>();
        }

        List<FindUserByIdRspDTO> userList = response.getData();

        // 转换为 Map，key 为用户ID，value 为用户信息
        return userList.stream()
                .collect(Collectors.toMap(
                        FindUserByIdRspDTO::getId,
                        user -> user,
                        (existing, replacement) -> existing // 处理重复key
                ));
    }
}
