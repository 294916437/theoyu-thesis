-- thesis.t_conversation definition

CREATE TABLE `t_conversation` (
  `id` bigint unsigned NOT NULL COMMENT '会话ID',
  `conversation_type` tinyint unsigned NOT NULL DEFAULT '1' COMMENT '会话类型：1-单聊，2-群聊',
  `title` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '群聊标题（单聊时为NULL）',
  `last_message_id` bigint unsigned DEFAULT NULL COMMENT '最后一条消息ID（冗余字段，用于快速显示会话列表）',
  `last_message_time` datetime DEFAULT NULL COMMENT '最后一条消息时间（冗余字段，用于会话排序）',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_last_message_time` (`last_message_time` DESC) COMMENT '会话列表按时间倒序查询'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会话表';


-- thesis.t_conversation_participant definition

CREATE TABLE `t_conversation_participant` (
  `conversation_id` bigint unsigned NOT NULL COMMENT '会话ID',
  `user_id` bigint unsigned NOT NULL COMMENT '用户ID',
  `is_active` bit(1) NOT NULL DEFAULT b'1' COMMENT '是否激活：1-激活，0-已退出/已删除',
  `unread_count` int unsigned NOT NULL DEFAULT '0' COMMENT '未读消息数（冗余字段，提高查询效率）',
  `last_read_message_id` bigint unsigned DEFAULT NULL COMMENT '最后已读消息ID',
  `last_read_time` datetime DEFAULT NULL COMMENT '最后阅读时间',
  `joined_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
  `updated_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`conversation_id`,`user_id`),
  KEY `idx_user_active` (`user_id`,`is_active`) COMMENT '查询用户的活跃会话',
  KEY `idx_conversation_active` (`conversation_id`,`is_active`) COMMENT '查询会话的活跃参与者'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会话参与者表';


-- thesis.t_message definition

CREATE TABLE `t_message` (
  `id` bigint unsigned NOT NULL COMMENT '消息ID',
  `conversation_id` bigint unsigned NOT NULL COMMENT '会话ID',
  `sender_id` bigint unsigned NOT NULL COMMENT '发送者用户ID',
  `message_type` tinyint unsigned NOT NULL DEFAULT '1' COMMENT '消息类型：1-文本，2-图片，3-音频，4-视频，6-文件，9-系统消息',
  `img_uris` varchar(660) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '笔记图片链接(逗号隔开)',
  `video_uri` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '视频链接',
  `content_uuid` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '消息内容UUID',
  `reply_to_message_id` bigint unsigned DEFAULT NULL COMMENT '回复的消息ID（引用回复功能）',
  `is_deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否被删除：0-正常，1-已删除',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_conversation_id` (`conversation_id`,`id` DESC) COMMENT '会话消息列表（利用ID有序性）',
  KEY `idx_sender` (`sender_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消息表';


-- thesis.t_permission definition

CREATE TABLE `t_permission` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `parent_id` bigint unsigned NOT NULL DEFAULT '0' COMMENT '父ID',
  `name` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '权限名称',
  `type` tinyint unsigned NOT NULL COMMENT '类型(1：目录 2：菜单 3：按钮)',
  `menu_url` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '菜单路由',
  `menu_icon` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '菜单图标',
  `sort` int unsigned NOT NULL DEFAULT '0' COMMENT '管理系统中的显示顺序',
  `permission_key` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '权限标识',
  `status` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '状态(0：启用；1：禁用)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '逻辑删除(0：未删除 1：已删除)',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='权限表';


-- thesis.t_role definition

CREATE TABLE `t_role` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `role_name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '角色名',
  `role_key` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '角色唯一标识',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态(0：启用 1：禁用)',
  `sort` int unsigned NOT NULL DEFAULT '0' COMMENT '管理系统中的显示顺序',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最后一次更新时间',
  `is_deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '逻辑删除(0：未删除 1：已删除)',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_key` (`role_key`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色表';


-- thesis.t_role_permission_rel definition

CREATE TABLE `t_role_permission_rel` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `role_id` bigint unsigned NOT NULL COMMENT '角色ID',
  `permission_id` bigint unsigned NOT NULL COMMENT '权限ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '逻辑删除(0：未删除 1：已删除)',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户权限表';


-- thesis.t_room definition

CREATE TABLE `t_room` (
  `id` bigint unsigned NOT NULL COMMENT '房间ID，分布式ID',
  `room_no` varchar(6) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '房间号，用于展示和搜索的短号',
  `host_id` bigint unsigned NOT NULL COMMENT '主持人/创建者 User ID',
  `sfu_node_id` bigint unsigned NOT NULL DEFAULT '0' COMMENT '绑定的 SFU 节点 ID，0表示未分配',
  `title` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '会议主题',
  `type` tinyint unsigned NOT NULL DEFAULT '1' COMMENT '会议类型: 1-即时会议, 2-预约会议',
  `max_participants` int unsigned DEFAULT '15' COMMENT '会议最大参与者数量',
  `status` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '状态: 0-预约中, 1-进行中, 2-已结束, 3-已取消',
  `start_time` datetime DEFAULT NULL COMMENT '实际开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '实际结束时间',
  `settings` json DEFAULT NULL COMMENT '房间配置快照 (如: 是否允许全员开麦, 是否录制)',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_room_no` (`room_no`),
  KEY `idx_host_status` (`host_id`,`status`) COMMENT '查询某人创建的会议',
  KEY `idx_sfu_status` (`sfu_node_id`,`status`) COMMENT '查询某节点上正在进行的会议'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='视频会议房间表';


-- thesis.t_room_message definition

CREATE TABLE `t_room_message` (
  `id` bigint unsigned NOT NULL COMMENT '消息ID',
  `room_id` bigint unsigned NOT NULL COMMENT '房间ID',
  `sender_id` bigint unsigned NOT NULL COMMENT '发送者 User ID',
  `message_type` tinyint unsigned NOT NULL DEFAULT '2' COMMENT '消息类型(1-系统消息 2-用户消息)',
  `content_type` tinyint unsigned NOT NULL DEFAULT '1' COMMENT '消息内容类型(1-文本 2-图片 3-文件)',
  `content` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '消息内容',
  `is_recalled` tinyint(1) DEFAULT '0' COMMENT '是否已撤回',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
  `updated_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_room_type` (`room_id`,`message_type`) COMMENT '用于查找指定房间的消息'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会议聊天消息表';


-- thesis.t_room_participant definition

CREATE TABLE `t_room_participant` (
  `room_id` bigint unsigned NOT NULL COMMENT '房间ID',
  `user_id` bigint unsigned NOT NULL COMMENT '用户ID',
  `role` tinyint unsigned NOT NULL DEFAULT '1' COMMENT '角色: 1-普通成员, 2-主持人, 3-联席主持',
  `status` tinyint unsigned NOT NULL DEFAULT '1' COMMENT '状态: 1-在线, 2-离线, 3-被移除',
  `audio_muted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否音频静音: 0-否, 1-是',
  `video_muted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否视频关闭: 0-否, 1-是',
  `joined_at` datetime DEFAULT NULL COMMENT '加入时间',
  `left_at` datetime DEFAULT NULL COMMENT '离开时间',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`room_id`,`user_id`),
  KEY `idx_user_history` (`user_id`,`joined_at`) COMMENT '查询用户的参会历史'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='参会人员记录表';


-- thesis.t_room_record definition

CREATE TABLE `t_room_record` (
  `room_id` bigint unsigned NOT NULL COMMENT '会议房间ID',
  `host_id` bigint unsigned NOT NULL COMMENT '发起录制的主持人ID',
  `file_url` varchar(255) DEFAULT NULL COMMENT '下载链接',
  `file_size` int DEFAULT '0' COMMENT '文件大小 (MB)',
  `duration` int DEFAULT '0' COMMENT '视频时长 (秒)',
  `format` varchar(16) DEFAULT 'mp4' COMMENT '视频格式',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态: 0-录制中(RECORDING), 1-上传中(UPLOADING), 2-已完成(COMPLETED), 3-失败(FAILED)',
  `start_time` datetime NOT NULL COMMENT '录制开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '录制结束时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`room_id`,`host_id`),
  KEY `idx_file_url` (`file_url`) COMMENT '查询会议关联的录制'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='会议录制记录表';


-- thesis.t_sfu_node definition

CREATE TABLE `t_sfu_node` (
  `id` bigint unsigned NOT NULL COMMENT '节点ID，分布式ID',
  `instance_id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '实例唯一标识',
  `ip_address` varchar(45) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '节点内网IP地址 (支持 IPv6)',
  `grpc_port` int unsigned NOT NULL COMMENT 'gRPC 服务端口',
  `http_port` int unsigned NOT NULL DEFAULT '0' COMMENT 'HTTP/WebSocket 端口',
  `region` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'default' COMMENT '所属区域/机房',
  `status` tinyint unsigned NOT NULL DEFAULT '1' COMMENT '状态: 0-下线, 1-正常, 2-高负载(不分配新房)',
  `current_load` int unsigned NOT NULL DEFAULT '0' COMMENT '当前承载的房间数/流数量',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
  `updated_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '心跳/状态更新时间',
  `grpc_host` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'gRPC 服务地址',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_instance_id` (`instance_id`),
  KEY `idx_status_load` (`status`,`current_load`) COMMENT '用于查找低负载的可用节点'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='SFU媒体服务器节点表';


-- thesis.t_user definition

CREATE TABLE `t_user` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `user_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '()',
  `password` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `nickname` varchar(24) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `avatar` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `birthday` date DEFAULT NULL,
  `background_img` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `phone` varchar(11) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `sex` tinyint DEFAULT '0' COMMENT '(0 1)',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '(0 1)',
  `introduction` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `is_deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '(0 1)',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_phone` (`phone`),
  UNIQUE KEY `uk_user_id` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=7015 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- thesis.t_user_role_rel definition

CREATE TABLE `t_user_role_rel` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint unsigned NOT NULL COMMENT '用户ID',
  `role_id` bigint unsigned NOT NULL COMMENT '角色ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '逻辑删除(0：未删除 1：已删除)',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=41 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC COMMENT='用户角色表';