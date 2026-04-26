package com.legal.chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.legal.chat.entity.ChatMessageEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessageEntity> {
}
