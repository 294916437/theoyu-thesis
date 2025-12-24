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
     * AppId
     */
    private String userAppId;

    /**
     * 性别
     */
    private Integer sex;
    /**
     * 手机号码
     */
    private String phone;

    /**
     * 岁数
     */
    private Integer age;

    /**
     * 生日
     */
    private LocalDate birthday;

    /**
     * 背景图片
     */
    private String backgroundImg;

    /**
     * 个人介绍
     */
    private String introduction;

}
