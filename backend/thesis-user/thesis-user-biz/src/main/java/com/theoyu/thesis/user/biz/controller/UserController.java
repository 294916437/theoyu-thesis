package com.theoyu.thesis.user.biz.controller;

import com.theoyu.framework.common.response.Response;
import com.theoyu.framework.logger.aspect.ApiOperationLog;
import com.theoyu.thesis.user.biz.model.vo.FindUserProfileReqVO;
import com.theoyu.thesis.user.biz.model.vo.FindUserProfileRspVO;
import com.theoyu.thesis.user.biz.model.vo.UpdateUserInfoReqVO;
import com.theoyu.thesis.user.biz.service.UserOnlineService;
import com.theoyu.thesis.user.biz.service.UserService;
import com.theoyu.thesis.user.dto.request.*;
import com.theoyu.thesis.user.dto.response.CheckUserOnlineRspDTO;
import com.theoyu.thesis.user.dto.response.FindUserByIdRspDTO;
import com.theoyu.thesis.user.dto.response.FindUserByPhoneRspDTO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/user")
@Slf4j
public class UserController {
    @Resource
    private UserService userService;
    @Resource
    private UserOnlineService userOnlineService;

    /**
     * 用户信息修改
     *
     * @param updateUserInfoReqVO
     * @return
     */
    @PostMapping(value = "/update", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Response<?> updateUserInfo(@Validated UpdateUserInfoReqVO updateUserInfoReqVO) {
        return userService.updateUserInfo(updateUserInfoReqVO);
    }

    @PostMapping("/register")
    @ApiOperationLog(description = "用户注册")
    public Response<Long> register(@Validated @RequestBody RegisterUserReqDTO registerUserReqDTO) {
        return userService.register(registerUserReqDTO);
    }

    @PostMapping("/findByPhone")
    @ApiOperationLog(description = "手机号查询用户信息")
    public Response<FindUserByPhoneRspDTO> findByPhone(@Validated @RequestBody FindUserByPhoneReqDTO findUserByPhoneReqDTO) {
        return userService.findByPhone(findUserByPhoneReqDTO);
    }

    @PostMapping("/password/update")
    @ApiOperationLog(description = "密码更新")
    public Response<?> updatePassword(@Validated @RequestBody UpdateUserPasswordReqDTO updateUserPasswordReqDTO) {
        return userService.updatePassword(updateUserPasswordReqDTO);
    }
    @PostMapping("/findById")
    @ApiOperationLog(description = "查询用户信息")
    public Response<FindUserByIdRspDTO> findById(@Validated @RequestBody FindUserByIdReqDTO findUserByIdReqDTO) {
        return userService.findById(findUserByIdReqDTO);
    }
    @PostMapping("/findByIds")
    @ApiOperationLog(description = "批量查询用户信息")
    public Response<List<FindUserByIdRspDTO>> findByIds(@Validated @RequestBody FindUsersByIdsReqDTO findUsersByIdsReqDTO) {
        return userService.findByIds(findUsersByIdsReqDTO);
    }

    @PostMapping(value = "/profile")
    public Response<FindUserProfileRspVO> findUserProfile(@Validated @RequestBody FindUserProfileReqVO findUserProfileReqVO) {
        return userService.findUserProfile(findUserProfileReqVO);
    }
    /**
     * 设置用户在线状态
     */
    @PostMapping("/online/set")
    @ApiOperationLog(description = "设置用户在线状态")
    public Response<?> setUserOnline(@Validated @RequestBody SetUserOnlineReqDTO setUserOnlineReqDTO) {
        userOnlineService.setUserOnline(setUserOnlineReqDTO.getUserId());
        return Response.success();
    }

    /**
     * 设置用户离线状态
     */
    @PostMapping("/offline/set")
    @ApiOperationLog(description = "设置用户离线状态")
    public Response<?> setUserOffline(@Validated @RequestBody SetUserOfflineReqDTO setUserOfflineReqDTO) {
        userOnlineService.setUserOffline(setUserOfflineReqDTO.getUserId());
        return Response.success();
    }

    /**
     * 检查用户是否在线
     */
    @PostMapping("/online/check")
    @ApiOperationLog(description = "检查用户是否在线")
    public Response<CheckUserOnlineRspDTO> checkUserOnline(@Validated @RequestBody CheckUserOnlineReqDTO checkUserOnlineReqDTO) {
        boolean online = userOnlineService.isUserOnline(checkUserOnlineReqDTO.getUserId());
        CheckUserOnlineRspDTO rspDTO = CheckUserOnlineRspDTO.builder()
                .userId(checkUserOnlineReqDTO.getUserId())
                .online(online)
                .build();
        return Response.success(rspDTO);
    }



}
