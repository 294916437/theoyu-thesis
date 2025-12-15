package com.theoyu.thesis.user.biz.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.RandomUtil;
import com.alibaba.nacos.shaded.com.google.common.base.Preconditions;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.collect.Lists;
import com.theoyu.framework.common.enums.DeletedEnum;
import com.theoyu.framework.common.enums.StatusEnum;
import com.theoyu.framework.common.exception.BusinessException;
import com.theoyu.framework.common.response.Response;
import com.theoyu.framework.common.utils.DateUtils;
import com.theoyu.framework.common.utils.JsonUtils;
import com.theoyu.framework.common.utils.ParamUtils;
import com.theoyu.framework.context.holder.LoginUserContextHolder;
import com.theoyu.thesis.user.biz.constants.MQConstants;
import com.theoyu.thesis.user.biz.constants.RedisKeyConstants;
import com.theoyu.thesis.user.biz.constants.RoleConstants;
import com.theoyu.thesis.user.biz.enums.ResponseCodeEnum;
import com.theoyu.thesis.user.biz.enums.SexEnum;
import com.theoyu.thesis.user.biz.model.entity.RolePO;
import com.theoyu.thesis.user.biz.model.entity.UserCountPO;
import com.theoyu.thesis.user.biz.model.entity.UserPO;
import com.theoyu.thesis.user.biz.model.entity.UserRolePO;
import com.theoyu.thesis.user.biz.model.mapper.RolePOMapper;
import com.theoyu.thesis.user.biz.model.mapper.UserCountPOMapper;
import com.theoyu.thesis.user.biz.model.mapper.UserPOMapper;
import com.theoyu.thesis.user.biz.model.mapper.UserRolePOMapper;
import com.theoyu.thesis.user.biz.model.vo.FindUserProfileReqVO;
import com.theoyu.thesis.user.biz.model.vo.FindUserProfileRspVO;
import com.theoyu.thesis.user.biz.model.vo.UpdateUserInfoReqVO;
import com.theoyu.thesis.user.biz.rpc.OssRpcService;
import com.theoyu.thesis.user.biz.rpc.IdGeneratorRpcService;
import com.theoyu.thesis.user.biz.service.UserService;
import com.theoyu.thesis.user.dto.request.*;
import com.theoyu.thesis.user.dto.response.FindUserByIdRspDTO;
import com.theoyu.thesis.user.dto.response.FindUserByPhoneRspDTO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j
public class UserServiceImpl implements UserService {
    @Resource
    private IdGeneratorRpcService idGeneratorRpcService;
    @Resource
    private UserPOMapper userPOMapper;
    @Resource
    private OssRpcService ossRpcService;
    @Resource
    private UserRolePOMapper userRolePOMapper;
    @Resource
    private RolePOMapper rolePOMapper;
    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Resource
    private RocketMQTemplate rocketMQTemplate;
    @Resource(name = "taskExecutor")
    private ThreadPoolTaskExecutor threadPoolTaskExecutor;
    /**
     * 用户ID缓存
     */
    private static final Cache<Long, FindUserByIdRspDTO> LOCAL_CACHE = Caffeine.newBuilder()
            .initialCapacity(10000) // 设置初始容量为 10000 个条目
            .maximumSize(10000) // 设置缓存的最大容量为 10000 个条目
            .expireAfterWrite(1, TimeUnit.HOURS) // 设置缓存条目在写入后 1 小时过期
            .build();

    /**
     * 用户主页信息本地缓存
     */
    private static final Cache<Long, FindUserProfileRspVO> PROFILE_LOCAL_CACHE = Caffeine.newBuilder()
            .initialCapacity(10000) // 设置初始容量为 10000 个条目
            .maximumSize(10000) // 设置缓存的最大容量为 10000 个条目
            .expireAfterWrite(5, TimeUnit.MINUTES) // 设置缓存条目在写入后 5 分钟过期
            .build();


    @Override
    public Response<?> updateUserInfo(UpdateUserInfoReqVO updateUserInfoReqVO) {
        // 检验是否是用户本人
        Long userId = updateUserInfoReqVO.getUserId();
        Long loginUserId = LoginUserContextHolder.getUserId();
        if (!Objects.equals(userId, loginUserId)) {
            throw new BusinessException(ResponseCodeEnum.CANT_UPDATE_OTHER_USER_PROFILE);
        }

        UserPO userPO = new UserPO();
        // 设置当前需要更新的用户 ID
        userPO.setId(LoginUserContextHolder.getUserId());
        // 标识位：是否需要更新
        boolean needUpdate = false;

        // 头像
        MultipartFile avatarFile = updateUserInfoReqVO.getAvatar();

        if (Objects.nonNull(avatarFile)) {
            String avatarUrl = ossRpcService.uploadFile(avatarFile);
            log.info("==> 上传头像成功，头像地址：{}", avatarUrl);
            if (StringUtils.isBlank(avatarUrl)) {
                throw new BusinessException(ResponseCodeEnum.UPLOAD_AVATAR_FAIL);
            }
            userPO.setAvatar(avatarUrl);
            needUpdate = true;
        }
        // 背景图
        MultipartFile backgroundImgFile = updateUserInfoReqVO.getBackgroundImg();
        if (Objects.nonNull(backgroundImgFile)) {
            String backgroundImgUrl = ossRpcService.uploadFile(backgroundImgFile);
            log.info("==> 上传背景图成功，背景图地址：{}", backgroundImgUrl);
            if (StringUtils.isBlank(backgroundImgUrl)) {
                throw new BusinessException(ResponseCodeEnum.UPLOAD_BACKGROUND_IMG_FAIL);
            }
            userPO.setBackgroundImg(backgroundImgUrl);
            needUpdate = true;
        }
        // 昵称
        String nickname = updateUserInfoReqVO.getNickname();
        if (StringUtils.isNotBlank(nickname)) {
            Preconditions.checkArgument(ParamUtils.checkNickname(nickname), ResponseCodeEnum.NICK_NAME_VALID_FAIL.getErrorMessage());
            userPO.setNickname(nickname);
            needUpdate = true;
        }

        // 用户应用ID
        String userAppId = updateUserInfoReqVO.getUserAppId();
        if (StringUtils.isNotBlank(userAppId)) {
            Preconditions.checkArgument(ParamUtils.checkUserAppId(userAppId), ResponseCodeEnum.USER_APP_ID_VALID_FAIL.getErrorMessage());
            userPO.setUserId(userAppId);
            needUpdate = true;
        }

        // 性别
        Integer sex = updateUserInfoReqVO.getSex();
        if (Objects.nonNull(sex)) {
            Preconditions.checkArgument(SexEnum.isValid(sex), ResponseCodeEnum.SEX_VALID_FAIL.getErrorMessage());
            userPO.setSex(sex);
            needUpdate = true;
        }

        // 生日
        LocalDate birthday = updateUserInfoReqVO.getBirthday();
        if (Objects.nonNull(birthday)) {
            userPO.setBirthday(birthday);
            needUpdate = true;
        }

        // 个人简介
        String introduction = updateUserInfoReqVO.getIntroduction();
        if (StringUtils.isNotBlank(introduction)) {
            Preconditions.checkArgument(ParamUtils.checkLength(introduction, 100), ResponseCodeEnum.INTRODUCTION_VALID_FAIL.getErrorMessage());
            userPO.setIntroduction(introduction);
            needUpdate = true;
        }


        if (needUpdate) {
            // 删除用户缓存
            deleteUserRedisCache(userId);
            // 更新用户信息
            userPO.setUpdateTime(LocalDateTime.now());
            userPOMapper.updateByPrimaryKeySelective(userPO);
            // 延迟双删
            sendDelayDeleteUserRedisCacheMQ(userId);

        }
        return Response.success();
    }

    /**
     * 异步发送延时消息
     * @param userId
     */
    private void sendDelayDeleteUserRedisCacheMQ(Long userId) {
        Message<String> message = MessageBuilder.withPayload(String.valueOf(userId))
                .build();

        rocketMQTemplate.asyncSend(MQConstants.TOPIC_DELAY_DELETE_USER_REDIS_CACHE, message,
                new SendCallback() {
                    @Override
                    public void onSuccess(SendResult sendResult) {
                        log.info("## 延时删除 Redis 用户缓存消息发送成功...");
                    }

                    @Override
                    public void onException(Throwable e) {
                        log.error("## 延时删除 Redis 用户缓存消息发送失败...", e);
                    }
                },
                3000, // 超时时间
                1 // 延迟级别，1 表示延时 1s
        );
    }

    /**
     * 删除 Redis 中的用户缓存
     * @param userId
     */
    private void deleteUserRedisCache(Long userId) {
        // 构建 Redis Key
        String userInfoRedisKey = RedisKeyConstants.buildUserInfoKey(userId);
        String userProfileRedisKey = RedisKeyConstants.buildUserProfileKey(userId);

        // 批量删除
        redisTemplate.delete(Arrays.asList(userInfoRedisKey, userProfileRedisKey));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Response<Long> register(RegisterUserReqDTO registerUserReqDTO) {
        String phone = registerUserReqDTO.getPhone();

        // 先判断该手机号是否已被注册
        UserPO userPO1 = userPOMapper.selectByPhone(phone);

        log.info("==> 用户是否注册, phone: {}, userPO: {}", phone, JsonUtils.toJsonString(userPO1));

        // 若已注册，则直接返回用户 ID
        if (Objects.nonNull(userPO1)) {
            return Response.success(userPO1.getId());
        }

        // 否则注册新用户
        String userAppId = idGeneratorRpcService.getUserAppId();
        String userIdStr = idGeneratorRpcService.getUserId();
        Long userId = Long.valueOf(userIdStr);
        UserPO userPO = UserPO.builder()
                .id(userId)
                .phone(phone)
                .userId(userAppId) // 自动生成的用户凭证
                .nickname("新用户") // 自动生成昵称
                .status(StatusEnum.ENABLE.getValue()) // 状态为启用
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .isDeleted(DeletedEnum.NO.getValue()) // 逻辑删除
                .build();

        // 添加入库
        userPOMapper.insert(userPO);

        // 给该用户分配一个默认角色
        UserRolePO userRolePO = UserRolePO.builder()
                .userId(userId)
                .roleId(RoleConstants.COMMON_USER_ROLE_ID)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .isDeleted(DeletedEnum.NO.getValue())
                .build();
        userRolePOMapper.insert(userRolePO);

        RolePO rolePO = rolePOMapper.selectByPrimaryKey(RoleConstants.COMMON_USER_ROLE_ID);

        // 将该用户的角色 ID 存入 Redis 中
        List<String> roles = new ArrayList<>(1);
        roles.add(rolePO.getRoleKey());

        String userRolesKey = RedisKeyConstants.buildUserRoleKey(userId);
        redisTemplate.opsForValue().set(userRolesKey, JsonUtils.toJsonString(roles));

        return Response.success(userId);
    }

    @Override
    public Response<FindUserByPhoneRspDTO> findByPhone(FindUserByPhoneReqDTO findUserByPhoneReqDTO) {
        String phone = findUserByPhoneReqDTO.getPhone();

        // 根据手机号查询用户信息
        UserPO userPO = userPOMapper.selectByPhone(phone);

        // 判空
        if (Objects.isNull(userPO)) {
            throw new BusinessException(ResponseCodeEnum.USER_NOT_FOUND);
        }

        // 构建返参
        FindUserByPhoneRspDTO findUserByPhoneRspDTO = FindUserByPhoneRspDTO.builder()
                .id(userPO.getId())
                .password(userPO.getPassword())
                .build();

        return Response.success(findUserByPhoneRspDTO);

    }

    @Override
    public Response<?> updatePassword(UpdateUserPasswordReqDTO updateUserPasswordReqDTO) {
        // 获取当前请求对应的用户 ID
        Long userId = LoginUserContextHolder.getUserId();

        UserPO userPO = UserPO.builder()
                .id(userId)
                .password(updateUserPasswordReqDTO.getEncodePassword()) // 加密后的密码
                .updateTime(LocalDateTime.now())
                .build();
        // 更新密码
        userPOMapper.updateByPrimaryKeySelective(userPO);

        return Response.success();
    }

    @Override
    public Response<FindUserByIdRspDTO> findById(FindUserByIdReqDTO findUserByIdReqDTO) {
        Long userId = findUserByIdReqDTO.getId();
        FindUserByIdRspDTO cachedUser = LOCAL_CACHE.getIfPresent(userId);
        if(Objects.nonNull(cachedUser)) {
            log.info("==> 命中本地缓存；{}", cachedUser);
            return Response.success(cachedUser);
        }
        // 用户缓存 Redis Key
        String userInfoRedisKey = RedisKeyConstants.buildUserInfoKey(userId);

        // 先从 Redis 缓存中查询
        String userInfoRedisValue = (String) redisTemplate.opsForValue().get(userInfoRedisKey);

        // 若 Redis 缓存中存在该用户信息
        if (StringUtils.isNotBlank(userInfoRedisValue)) {
            // 将存储的 Json 字符串转换成对象，并返回
            FindUserByIdRspDTO findUserByIdRspDTO = JsonUtils.parseObject(userInfoRedisValue, FindUserByIdRspDTO.class);
            // 写入本地缓存
            threadPoolTaskExecutor.submit(()->{
                if(Objects.nonNull(findUserByIdRspDTO)) {
                    LOCAL_CACHE.put(userId, findUserByIdRspDTO);
                }
            }) ;
            return Response.success(findUserByIdRspDTO);
        }

        // 否则, 从数据库中查询用户信息
        UserPO userPO = userPOMapper.selectByPrimaryKey(userId);

        if (Objects.isNull(userPO)) {
            threadPoolTaskExecutor.execute(() -> {
                // 防止缓存穿透，将空数据存入 Redis 缓存 (过期时间不宜设置过长)
                // 保底1分钟 + 随机秒数
                long expireSeconds = 60 + RandomUtil.randomInt(60);
                redisTemplate.opsForValue().set(userInfoRedisKey, "null", expireSeconds, TimeUnit.SECONDS);
            });
            throw new BusinessException(ResponseCodeEnum.USER_NOT_FOUND);
        }

        // 构建返参
        FindUserByIdRspDTO findUserByIdRspDTO = FindUserByIdRspDTO.builder()
                .id(userPO.getId())
                .nickName(userPO.getNickname())
                .avatar(userPO.getAvatar())
                .introduction(userPO.getIntroduction())
                .build();

        // 异步将用户信息存入 Redis 缓存，提升响应速度
        threadPoolTaskExecutor.submit(() -> {
            // 过期时间（保底1天 + 随机秒数）
            long expireSeconds = 60*60*24 + RandomUtil.randomInt(60*60*24);
            redisTemplate.opsForValue()
                    .set(userInfoRedisKey, JsonUtils.toJsonString(findUserByIdRspDTO), expireSeconds, TimeUnit.SECONDS);
        });

        return Response.success(findUserByIdRspDTO);
    }

    @Override
    public Response<List<FindUserByIdRspDTO>> findByIds(FindUsersByIdsReqDTO findUsersByIdsReqDTO) {
        // 需要查询的用户 ID 集合
        List<Long> userIds = findUsersByIdsReqDTO.getIds();

        // 构建 Redis Key 集合
        List<String> redisKeys = userIds.stream()
                .map(RedisKeyConstants::buildUserInfoKey)
                .toList();

        // 先从 Redis 缓存中查, multiGet 批量查询提升性能
        List<Object> redisValues = redisTemplate.opsForValue().multiGet(redisKeys);
        // 如果缓存中不为空
        if (CollUtil.isNotEmpty(redisValues)) {
            // 过滤掉为空的数据
            redisValues = redisValues.stream().filter(Objects::nonNull).toList();
        }

        // 返参
        List<FindUserByIdRspDTO> findUserByIdRspDTOS = Lists.newArrayList();

        // 将过滤后的缓存集合，转换为 DTO 返参实体类
        if (CollUtil.isNotEmpty(redisValues)) {
            findUserByIdRspDTOS = redisValues.stream()
                    .map(value -> JsonUtils.parseObject(String.valueOf(value), FindUserByIdRspDTO.class))
                    .collect(Collectors.toList());
        }

        // 如果被查询的用户信息，都在 Redis 缓存中, 则直接返回
        if (CollUtil.size(userIds) == CollUtil.size(findUserByIdRspDTOS)) {
            return Response.success(findUserByIdRspDTOS);
        }

        // 还有另外两种情况：一种是缓存里没有用户信息数据，还有一种是缓存里数据不全，需要从数据库中补充
        // 筛选出缓存里没有的用户数据，去查数据库
        List<Long> userIdsNeedQuery = null;

        if (CollUtil.isNotEmpty(findUserByIdRspDTOS)) {
            // 将 findUserInfoByIdRspDTOS 集合转 Map
            Map<Long, FindUserByIdRspDTO> map = findUserByIdRspDTOS.stream()
                    .collect(Collectors.toMap(FindUserByIdRspDTO::getId, p -> p));

            // 筛选出需要查 DB 的用户 ID
            userIdsNeedQuery = userIds.stream()
                    .filter(id -> Objects.isNull(map.get(id)))
                    .toList();
        } else { // 缓存中一条用户信息都没查到，则提交的用户 ID 集合都需要查数据库
            userIdsNeedQuery = userIds;
        }

        // 从数据库中批量查询
        List<UserPO> userPOS = userPOMapper.selectByIds(userIdsNeedQuery);

        List<FindUserByIdRspDTO> findUserByIdRspDTOS2 = null;

        // 若数据库查询的记录不为空
        if (CollUtil.isNotEmpty(userPOS)) {
            // DO 转 DTO
            findUserByIdRspDTOS2 = userPOS.stream()
                    .map(userPO -> FindUserByIdRspDTO.builder()
                            .id(userPO.getId())
                            .nickName(userPO.getNickname())
                            .avatar(userPO.getAvatar())
                            .introduction(userPO.getIntroduction())
                            .build())
                    .collect(Collectors.toList());

            // 异步线程将用户信息同步到 Redis 中
            List<FindUserByIdRspDTO> finalFindUserByIdRspDTOS = findUserByIdRspDTOS2;
            threadPoolTaskExecutor.submit(() -> {
                // DTO 集合转 Map
                Map<Long, FindUserByIdRspDTO> map = finalFindUserByIdRspDTOS.stream()
                        .collect(Collectors.toMap(FindUserByIdRspDTO::getId, p -> p));

                // 执行 pipeline 操作
                redisTemplate.executePipelined(new SessionCallback<>() {
                    @Override
                    public Object execute(RedisOperations operations) {
                        for (UserPO userPO : userPOS) {
                            Long userId = userPO.getId();

                            // 用户信息缓存 Redis Key
                            String userInfoRedisKey = RedisKeyConstants.buildUserInfoKey(userId);

                            // DTO 转 JSON 字符串
                            FindUserByIdRspDTO findUserInfoByIdRspDTO = map.get(userId);
                            String value = JsonUtils.toJsonString(findUserInfoByIdRspDTO);

                            // 过期时间（保底1天 + 随机秒数，将缓存过期时间打散，防止同一时间大量缓存失效，导致数据库压力太大）
                            long expireSeconds = 60*60*24 + RandomUtil.randomInt(60*60*24);
                            operations.opsForValue().set(userInfoRedisKey, value, expireSeconds, TimeUnit.SECONDS);
                        }
                        return null;
                    }
                });
            });
        }

        // 合并数据
        if (CollUtil.isNotEmpty(findUserByIdRspDTOS2)) {
            findUserByIdRspDTOS.addAll(findUserByIdRspDTOS2);
        }

        return Response.success(findUserByIdRspDTOS);
    }

    /**
     * 获取用户主页信息
     *
     * @return
     */
    @Override
    public Response<FindUserProfileRspVO> findUserProfile(FindUserProfileReqVO findUserProfileReqVO) {
        Long userId = findUserProfileReqVO.getUserId();

        if (Objects.isNull(userId)) {
            userId = LoginUserContextHolder.getUserId();
        }
        // 首先查询本地缓存
        // 对于非本人用户查看主页，走本地缓存；因此，本人用户查看主页，不走本地缓存
        if (!Objects.equals(userId, LoginUserContextHolder.getUserId())) {

            FindUserProfileRspVO userProfileLocalCache = PROFILE_LOCAL_CACHE.getIfPresent(userId);
            if (Objects.nonNull(userProfileLocalCache)) {
                log.info("## 用户主页信息命中本地缓存: {}", JsonUtils.toJsonString(userProfileLocalCache));
                return Response.success(userProfileLocalCache);
            }
        }

        // 从Redis缓存中查
        String userProfileRedisKey = RedisKeyConstants.buildUserProfileKey(userId);

        String userProfileJson = (String) redisTemplate.opsForValue().get(userProfileRedisKey);

        if (StringUtils.isNotBlank(userProfileJson)) {
            FindUserProfileRspVO findUserProfileRspVO = JsonUtils.parseObject(userProfileJson, FindUserProfileRspVO.class);
            // 同步到本地缓存
            syncUserProfile2LocalCache(userId, findUserProfileRspVO);

            return Response.success(findUserProfileRspVO);
        }


        UserPO userPO = userPOMapper.selectByPrimaryKey(userId);

        if (Objects.isNull(userPO)) {
            throw new BusinessException(ResponseCodeEnum.USER_NOT_FOUND);
        }

        FindUserProfileRspVO findUserProfileRspVO = FindUserProfileRspVO.builder()
                .userId(userPO.getId())
                .avatar(userPO.getAvatar())
                .nickname(userPO.getNickname())
                .userAppId(userPO.getUserId())
                .sex(userPO.getSex())
                .phone(userPO.getPhone())
                .introduction(userPO.getIntroduction())
                .build();

        LocalDate birthday = userPO.getBirthday();
        if (Objects.nonNull(birthday)) {
            findUserProfileRspVO.setAge(DateUtils.calculateAge(birthday));
            findUserProfileRspVO.setBirthday(birthday);
        }
        // 同步到本地缓存
        syncUserProfile2LocalCache(userId, findUserProfileRspVO);
        // 同步到Redis缓存中
        syncUserProfile2Redis(userProfileRedisKey, findUserProfileRspVO);

        return Response.success(findUserProfileRspVO);
    }

    /**
     * 异步同步到本地缓存
     *
     * @param userId
     * @param findUserProfileRspVO
     */
    private void syncUserProfile2LocalCache(Long userId, FindUserProfileRspVO findUserProfileRspVO) {
        threadPoolTaskExecutor.submit(() -> {
            PROFILE_LOCAL_CACHE.put(userId, findUserProfileRspVO);
        });
    }

    /**
     * 异步同步到 Redis 中
     *
     * @param userProfileRedisKey
     * @param findUserProfileRspVO
     */
    private void syncUserProfile2Redis(String userProfileRedisKey, FindUserProfileRspVO findUserProfileRspVO) {
        threadPoolTaskExecutor.submit(() -> {
            // 设置随机过期时间 (2小时以内)
            long expireTime = 60*60 + RandomUtil.randomInt(60 * 60);

            // 将 VO 转为 Json 字符串写入到 Redis 中
            redisTemplate.opsForValue().set(userProfileRedisKey, JsonUtils.toJsonString(findUserProfileRspVO), expireTime, TimeUnit.SECONDS);
        });
    }

}
