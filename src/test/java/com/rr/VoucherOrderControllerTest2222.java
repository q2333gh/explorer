package com.rr;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.RandomUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rr.dto.LoginFormDTO;
import com.rr.dto.Result;
import com.rr.entity.User;
import com.rr.service.IUserService;
import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import javax.annotation.Resource;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import static com.rr.utils.constants.RedisConstants.LOGIN_CODE_KEY;

/**
 * 使用前提：
 *  1.有hutool工具类的依赖
 *  2.登陆功能会返回验证码，用户不存在会自动注册，且用phone字段来进行登陆。
 */
@SpringBootTest
@AutoConfigureMockMvc
class VoucherOrderControllerTest222 {

  @Resource
  private MockMvc mockMvc;

  @Resource
  private IUserService userService;

  @Resource
  private ObjectMapper mapper;

  @Resource
  private StringRedisTemplate stringRedisTemplate;

  public static int USER_NUMBER=50;


  @Test
  @SneakyThrows
  @DisplayName("创建50个用户到数据库")
  void createUser() {
    List<String> phoneList = new ArrayList<>();
    for (int i = 0; i < USER_NUMBER; i++) {
      String phone = String.format("131%s", RandomUtil.randomInt(10000000, 99999999));
      phoneList.add(phone);
    }
    ExecutorService executorService = ThreadUtil.newExecutor(phoneList.size());
    CountDownLatch countDownLatch = new CountDownLatch(phoneList.size());
    phoneList.forEach(phone -> {
      executorService.execute(() -> {
        try {
          codeAndLogin(phone);
        } catch (Exception e) {
          e.printStackTrace();
        } finally {
          countDownLatch.countDown();
        }
      });
    });
    countDownLatch.await();
    executorService.shutdown();
    System.out.println("创建: " + phoneList.size() + " 个用户成功");
  }

  @Test
  @SneakyThrows
  @DisplayName("登录50个用户，并输出到文件中")
  void login() {
    List<String> phoneList = userService.lambdaQuery()
        .select(User::getPhone)
        .last("limit "+USER_NUMBER)
        .list().stream().map(User::getPhone).collect(Collectors.toList());
    ExecutorService executorService = ThreadUtil.newExecutor(phoneList.size());
    List<String> tokenList = new CopyOnWriteArrayList<>();
    CountDownLatch countDownLatch = new CountDownLatch(phoneList.size());
    phoneList.forEach(phone -> {
      executorService.execute(() -> {
        try {
          String token = codeAndLogin(phone);
          tokenList.add(token);
          countDownLatch.countDown();
        } catch (Exception e) {
          e.printStackTrace();
        }
      });
    });
    countDownLatch.await();
    executorService.shutdown();
    Assert.isTrue(tokenList.size() == phoneList.size());
    writeToTxt(tokenList, "/tokens.txt");
    System.out.println("写入完成！");
  }

  /**
   * 获取验证码，且登陆
   * @param phone
   * @return token
   */
  private  String codeAndLogin(String phone) throws Exception {
    // 验证码
    String codeJson = mockMvc.perform(MockMvcRequestBuilders
            .post("/user/code")
            .queryParam("phone", phone))
        .andExpect(MockMvcResultMatchers.status().isOk())
        .andReturn().getResponse().getContentAsString();
    Result result = mapper.readerFor(Result.class).readValue(codeJson);
    Assert.isTrue(result.getSuccess(), String.format("获取“%s”手机号的验证码失败", phone));
    //String code = result.getData().toString();
    String code = stringRedisTemplate.opsForValue().get(LOGIN_CODE_KEY + phone);
    LoginFormDTO formDTO = new LoginFormDTO();
    formDTO.setCode(code);
    formDTO.setPhone(phone);
    String json = mapper.writeValueAsString(formDTO);
    // token
    String tokenJson = mockMvc.perform(MockMvcRequestBuilders
            .post("/user/login")
            .content(json)
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(MockMvcResultMatchers.status().isOk())
        .andReturn().getResponse().getContentAsString();
    result = mapper.readerFor(Result.class).readValue(tokenJson);
    Assert.isTrue(result.getSuccess(), String.format(
        "获取“%s”手机号的token失败,json为“%s”", phone, json));
    String token = result.getData().toString();
    return token;
  }

  //生成token文件
  private static void writeToTxt(List<String> list, String suffixPath) throws Exception {
    // 1. 创建文件
    File file = new File("src/main/resources" + suffixPath);
    if (!file.exists()) {
      file.createNewFile();
    }
    // 2. 输出
    BufferedWriter bw = new BufferedWriter(
        new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8));
    for (String content : list) {
      bw.write(content);
      bw.newLine();
    }
    bw.close();
    System.out.println("写入完成！");
  }
}