package com.explorer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.explorer.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.beans.factory.annotation.Autowired;


@Mapper
public interface UserMapper extends BaseMapper<User> {

}
