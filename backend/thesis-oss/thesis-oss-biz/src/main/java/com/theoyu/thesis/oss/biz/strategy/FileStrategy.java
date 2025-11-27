package com.theoyu.thesis.oss.biz.strategy;

import org.springframework.web.multipart.MultipartFile;

public interface  FileStrategy {

    /**
     * 上传文件
     * @param file
     * @param bucketName
     * @return
     */
    String uploadFile(MultipartFile file, String bucketName) ;
}
