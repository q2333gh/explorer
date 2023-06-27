package com.explorer;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.RandomUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.explorer.dto.LoginFormDTO;
import com.explorer.dto.Result;
import com.explorer.entity.User;
import com.explorer.service.IUserService;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;
import javax.annotation.Resource;
import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

/**
 * function describe: 创建用户到数据库,且登录,保存tokens到文件
 */
@SpringBootTest
@AutoConfigureMockMvc
class VoucherOrderControllerTest {

  private static final String PHONE_3digits = "155%s";
  private static int USER_NUMBER;//添加用户量
  @Resource
  private MockMvc mockMvc;
  @Resource
  private IUserService userService;
  @Resource
  private ObjectMapper mapper;

  private static String nowTime() {
    return DateTime.now().toString()
        .replace(" ", "_")
        .replace("-", "_")
        .replace(":", "_");
  }

  private static List<String> genUsersPhones() {
    List<String> phoneList = new ArrayList<>();
    for (int i = 0; i < USER_NUMBER; i++) {
      String phone = String.format(PHONE_3digits, getRandom8digits());
      phoneList.add(phone);
    }
    return phoneList;
  }

  private static int getRandom8digits() {
    return RandomUtil.randomInt(10000000, 99999999);
  }

  /**
   * 生成token文件
   */
  private static void writeToTxt(List<String> list, String fileName) throws Exception {
    String curDir = System.getProperty("user.dir");
    System.out.println(curDir);
    File dir = new File("src/main/resources/testTokens");
    //        如果src前面有个/  ->  绝对路径 , in windows : current drive of the Java process: C:/src...
    //        没有/  或者是./ 则是相对路径
    if (!dir.exists()) {
      boolean mkdirs = dir.mkdirs();
      if (!mkdirs) {
        System.out.println("mkdir error!");
        return;
      }
    }
    File file = new File(dir, fileName);
    FileWriter fileWriter = new FileWriter(file);
    BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
    StringBuilder sb = new StringBuilder();
    for (String item : list) {
      sb.append(item).append(",").append("\n");
      //            在jmeter识别.txt文件或者.csv文件的时候,必须要 换行! 我晕!
      //       JMeter, it will be treated as a single line containing multiple tokens separated by commas.
    }
    sb.deleteCharAt(sb.length() - 1);        // Remove the last comma
    bufferedWriter.write(sb.toString());
    bufferedWriter.close();
    fileWriter.close();
    //    System.out.println("写入完成！");
    //        System.out.println("文件保存至: "+System.getProperty("user.dir")+fileName);
  }

  @Test
  @SneakyThrows
  @DisplayName("创建用户到数据库,且登录,保存tokens到文件")
  void createUser2DB() {
    USER_NUMBER = 1000;
    List<String> phoneList = genUsersPhones();
    concurrentCreateUser(phoneList);
  }

  private void concurrentCreateUser(List<String> phoneList) throws Exception {
    ExecutorService executorService = ThreadUtil.newExecutor(phoneList.size());
    CountDownLatch countDownLatch = new CountDownLatch(phoneList.size());
    List<String> tokenList = new CopyOnWriteArrayList<>();
    phoneList.forEach(phone -> executorService.execute(() -> {
      try {
        String token = codeAndLogin(phone);
        tokenList.add(token);
      } catch (Exception e) {
        e.printStackTrace();
      } finally {
        countDownLatch.countDown();
      }
    }));
    countDownLatch.await();
    executorService.shutdown();
    System.out.println("创建: " + phoneList.size() + " 个用户成功");
    Assert.isTrue(tokenList.size() == phoneList.size());
    //        String fileName="tokens"+ "_"+nowTime() +".txt";
    String fileName = "tokens.txt";
    writeToTxt(tokenList, fileName);
  }

  private List<String> callDB2getPhones() {
    return userService.lambdaQuery()
        .select(User::getPhone)
        .last("limit " + USER_NUMBER)
        .list().stream().map(User::getPhone).collect(Collectors.toList());
  }

  /**
   * 获取验证码，且登陆
   */
  private String codeAndLogin(String phone) throws Exception {
    String code = getCode(phone);
    return loginAndRetToken(phone, code);
  }

  private String getCode(String phone) throws Exception {
    String codeJson = mockMvc.perform(MockMvcRequestBuilders
            .post("/user/code")
            .queryParam("phone", phone))
        .andExpect(MockMvcResultMatchers.status().isOk())
        .andReturn().getResponse().getContentAsString();
    Result result = JSON2Result(codeJson);
    Assert.isTrue(result.getSuccess(), String.format("获取“%s”手机号的验证码失败", phone));
    return result.getData().toString();
  }

  private String loginAndRetToken(String phone, String code) throws Exception {
    String json = genLoginJSON(phone, code);
    String tokenJson = mockMvc.perform(MockMvcRequestBuilders
            .post("/user/login")
            .content(json)
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(MockMvcResultMatchers.status().isOk())
        .andReturn().getResponse().getContentAsString();
    Result result = JSON2Result(tokenJson);
    Assert.isTrue(result.getSuccess(), String.format("获取“%s”手机号的token失败,json为“%s”",
        phone, json));
    return result.getData().toString();
  }

  private Result JSON2Result(String tokenJson) throws JsonProcessingException {
    return mapper.readerFor(Result.class).readValue(tokenJson);
  }

  private String genLoginJSON(String phone, String code) throws JsonProcessingException {
    LoginFormDTO formDTO = new LoginFormDTO();
    formDTO.setCode(code);
    formDTO.setPhone(phone);
    return mapper.writeValueAsString(formDTO);
  }


}