package com.theoyu.framework.common.constants;

public interface GlobalConstants {

    /**
     * 用户 ID
     */
    String USER_ID = "userId";

    /**
     * Minio 存储桶名称
     */
    String BUCKET_NAME = "thesis";
    /**
     * 最大文件大小 10MB
     */
    Long MAX_FILE_SIZE = (long) (10 * 1024 * 1024); // 最大文件大小限制
}
