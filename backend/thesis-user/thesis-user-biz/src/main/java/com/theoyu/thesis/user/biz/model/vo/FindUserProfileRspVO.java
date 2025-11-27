package com.theoyu.thesis.user.biz.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FindUserProfileRspVO {

    /**
     * 用户 ID
     */
    private Long userId;

    /**
     * 头像
     */
    private String avatar;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 小哈书 ID
     */
    private String userAppId;

    /**
     * 性别
     */
    private Integer sex;

    /**
     * 岁数
     */
    private Integer age;

    /**
     * 生日
     */
    private LocalDate birthday;

    /**
     * 个人介绍
     */
    private String introduction;

    /**
     * 关注数
     */
    private String followingTotal = "0";

    /**
     * 粉丝数
     */
    private String fansTotal = "0";

    /**
     * 点赞与收藏总数
     */
    private String likeAndCollectTotal = "0";

    /**
     * 笔记总数
     */
    private String noteTotal = "0";
    /**
     * 点赞总数
     */
    private String likeTotal = "0";
    /**
     * 收藏总数
     */
    private String collectTotal = "0";
}
