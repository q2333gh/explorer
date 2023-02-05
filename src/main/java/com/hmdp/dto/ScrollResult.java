package com.hmdp.dto;

import java.util.List;
import lombok.Data;

@Data
public class ScrollResult {

  private List<?> list;
  private Long minTime;
  private Integer offset;
}
