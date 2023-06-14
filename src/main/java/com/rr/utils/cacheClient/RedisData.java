package com.rr.utils.cacheClient;

import java.time.LocalDateTime;
import lombok.Data;

/**
 *
 */
@Data
public class RedisData {//enhance redis data structure with LET(in java)

  private LocalDateTime expireTime;//use for logical expire.not redisTTL
  private Object data;//link to the obj
}
