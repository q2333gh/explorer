package com.explorer.controller;

import static com.explorer.utils.constants.SystemConstants.IMAGE_UPLOAD_DIR;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.explorer.dto.Result;
import com.explorer.utils.constants.SystemConstants;
import java.io.File;
import java.io.IOException;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("upload")
public class UploadController {

  @PostMapping("blog")
  public Result uploadImage(@RequestParam("file") MultipartFile image) {
    try {
      // 获取原始文件名称
      String originalFilename = image.getOriginalFilename();
      // 生成新文件名
      String fileName = newFile(originalFilename);
      // 保存文件
      image.transferTo(new File(IMAGE_UPLOAD_DIR, fileName));
      // 返回结果
      log.debug("文件上传成功，{}", fileName);
      return Result.ok(fileName);
    } catch (IOException e) {
      throw new RuntimeException("文件上传失败", e);
    }
  }

  @GetMapping("/blog/delete")
  public Result deleteBlogImg(@RequestParam("name") String filename) {
    File file = new File(IMAGE_UPLOAD_DIR, filename);
    if (file.isDirectory()) {
      return Result.fail("错误的文件名称");
    }
    FileUtil.del(file);
    return Result.ok();
  }

  private String newFile(String originalFilename) {
    String suffix = StrUtil.subAfter(originalFilename, ".", true);
    String name = randomUUID();
    int hash = name.hashCode();
    int d1 = hash & 0xF;
    int d2 = (hash >> 4) & 0xF;
    File dir = new File(IMAGE_UPLOAD_DIR, StrUtil.format("/blogs/{}/{}", d1, d2));
    if (!dir.exists()) {
      try {
        dir.mkdirs();
      } catch (Exception e) {
        log.error("mkdir error!",e);
        throw new RuntimeException(e);
      }
    }
    return StrUtil.format("/blogs/{}/{}/{}.{}", d1, d2, name, suffix);
  }

  private static String randomUUID() {
    return UUID.randomUUID().toString();
  }
}
