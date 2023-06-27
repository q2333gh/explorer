package com.explorer.dto;

import lombok.Data;

//data transport object
@Data
public class LoginFormDTO {
  private String phone;
  private String code;
  private String password;
}
