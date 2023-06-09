package com.rr;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.RandomUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rr.dto.LoginFormDTO;
import com.rr.dto.Result;
import com.rr.entity.User;
import com.rr.service.IUserService;
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
 * 使用前提：
 *  1.有Hutool工具类的依赖
 *  2.登陆功能会返回验证码，用户不存在会自动注册，且用phone字段来进行登陆。
 */
@SpringBootTest
@AutoConfigureMockMvc
class VoucherOrderControllerTest {

    @Resource
    private MockMvc mockMvc;

    @Resource
    private IUserService userService;

    @Resource
    private ObjectMapper mapper;

    private static final int USER_NUMBER=1;//添加用户量
//    private static final int USER_NUMBER=100;//添加用户量




    @Test
    @SneakyThrows
    @DisplayName("创建用户到数据库")
    void createUser2DB() {
        List<String> phoneList = new ArrayList<>();
        for (int i = 0; i < USER_NUMBER; i++) {
            String phone = String.format("158%s", RandomUtil.randomInt(10000000, 99999999));
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
    @DisplayName("登录用户，并输出到文件中")
    void loginAndGenTokensFile() {

        List<String> phoneList = userService.lambdaQuery()
            .select(User::getPhone)
            .last("limit " + USER_NUMBER)
            .list().stream().map(User::getPhone).toList();
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
        String fileName="tokens"+ DateTime.now()+".txt";
        writeToTxt(tokenList, "./"+fileName);
        System.out.println("文件保存至: "+System.getProperty("user.dir")+fileName);
    }

    /**
     * 获取验证码，且登陆
     * @param phone
     * @return token
     */
    private  String codeAndLogin(String phone) throws Exception {
        String code = getCode(phone);
        return logingAndRetToken(phone, code);
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

    private String logingAndRetToken(String phone, String code) throws Exception {
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
        String json = mapper.writeValueAsString(formDTO);
        return json;
    }


    //生成token文件
    private static void writeToTxt(List<String> list, String suffixPath) throws Exception {
        // 1. 创建文件
        File file = new File("src/main/resources" + suffixPath);
        if (!file.exists()) {
            file.createNewFile();
        }
        // 2. 输出
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8));
        for (String content : list) {
            bw.write(content);
            bw.newLine();
        }
        bw.close();
        System.out.println("写入完成！");
    }
}