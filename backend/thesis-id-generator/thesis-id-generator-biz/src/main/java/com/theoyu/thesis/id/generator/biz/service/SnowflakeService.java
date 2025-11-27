package com.theoyu.thesis.id.generator.biz.service;

import com.theoyu.thesis.id.generator.biz.constant.Constants;
import com.theoyu.thesis.id.generator.biz.core.IDGen;
import com.theoyu.thesis.id.generator.biz.core.common.PropertyFactory;
import com.theoyu.thesis.id.generator.biz.core.common.Result;
import com.theoyu.thesis.id.generator.biz.core.common.Status;
import com.theoyu.thesis.id.generator.biz.core.common.ZeroIDGen;
import com.theoyu.thesis.id.generator.biz.core.snowflake.SnowflakeIDGenImpl;
import com.theoyu.thesis.id.generator.biz.exception.InitException;
import com.theoyu.thesis.id.generator.biz.exception.LeafServerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

@Service("SnowflakeService")
public class SnowflakeService {
    private Logger logger = LoggerFactory.getLogger(SnowflakeService.class);

    private IDGen idGen;

    public SnowflakeService() throws InitException {
        Properties properties = PropertyFactory.getProperties();
        boolean flag = Boolean.parseBoolean(properties.getProperty(Constants.LEAF_SNOWFLAKE_ENABLE, "true"));
        if (flag) {
            String zkAddress = properties.getProperty(Constants.LEAF_SNOWFLAKE_ZK_ADDRESS);
            int port = Integer.parseInt(properties.getProperty(Constants.LEAF_SNOWFLAKE_PORT));
            idGen = new SnowflakeIDGenImpl(zkAddress, port);
            if(idGen.init()) {
                logger.info("Snowflake Service Init Successfully");
            } else {
                throw new InitException("Snowflake Service Init Fail");
            }
        } else {
            idGen = new ZeroIDGen();
            logger.info("Zero ID Gen Service Init Successfully");
        }
    }

    /**
     * 批量生成ID
     *
     * @param key 业务标识
     * @param count 生成数量
     * @return ID列表
     */
    public List<String> getBatchIds(String key, int count) {
        List<String> ids = new ArrayList<>(count);

        for (int i = 0; i < count; i++) {
            Result result = idGen.get(key);

            // 检查生成状态
            if (result.getStatus().equals(Status.EXCEPTION)) {
                logger.error("批量生成ID失败，key={}, 已生成={}/{}, error={}",
                        key, i, count, result.toString());
                throw new LeafServerException("批量生成ID失败: " + result.toString());
            }

            ids.add(String.valueOf(result.getId()));
        }

        return ids;
    }



    public Result getId(String key) {
        return idGen.get(key);
    }
}
