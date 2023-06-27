package com.explorer.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;
import jodd.introspector.Mapper;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * <p>
 *
 * </p>
 *
 * @author b
 * @since 2021-12-22
 */

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("tb_user")
public class User implements Serializable {

  private static final long serialVersionUID = 1L;

  /**
   * 主键
   */
  @TableId(value = "id", type = IdType.AUTO)
  private Long id;

  /**
   * 手机号码
   */
  private String phone;

  /**
   * 密码，加密存储
   */
  private String password;

  /**
   * 昵称，默认是随机字符
   */
  private String nickName;

  /**
   * 用户头像
   */
  private String icon = "";

  /**
   * 创建时间
   */
  private LocalDateTime createTime;

  /**
   * 更新时间
   */
  private LocalDateTime updateTime;


}
