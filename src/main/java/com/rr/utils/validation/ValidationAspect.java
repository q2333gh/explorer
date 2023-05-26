package com.rr.utils.validation;

import com.rr.dto.Result;
import com.rr.utils.regexUtils.RegexUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class ValidationAspect {
  @Around("@annotation(ParamValidate)")
  public Object MethodsValidation(ProceedingJoinPoint joinPoint, ParamValidate ParamValidate) throws Throwable {
    // Get method arguments
    Object[] args = joinPoint.getArgs();
    String phoneParamName = ParamValidate.phoneParamName();

    // Get phone argument value
    String phone = null;
    MethodSignature signature = (MethodSignature) joinPoint.getSignature();
    String[] parameterNames = signature.getParameterNames();
    for (int i = 0; i < parameterNames.length; i++) {
      if (parameterNames[i].equals(phoneParamName)) {
        phone = (String) args[i];
        break;
      }
    }

    // Validate phone number
    if (phone != null && RegexUtils.isPhoneInvalid(phone)) {
      return Result.fail("手机号格式错误！");
    }

    // Validation passed, proceed with method execution
    return joinPoint.proceed();
  }
}
