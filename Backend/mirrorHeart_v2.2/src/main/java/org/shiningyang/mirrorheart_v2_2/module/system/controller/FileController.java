package org.shiningyang.mirrorheart_v2_2.module.system.controller;

import lombok.RequiredArgsConstructor;
import org.shiningyang.mirrorheart_v2_2.common.result.Result;
import org.shiningyang.mirrorheart_v2_2.module.system.service.IFileService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/system/file")
@RequiredArgsConstructor
public class FileController {

    private final IFileService fileService;

    /**
     * 通用文件上传
     * @param file 文件
     * @param scene 场景 (如 AVATAR, POST_IMG, AUDIO)
     * @return 文件URL
     */
    @PostMapping("/upload")
    public Result<String> upload(@RequestParam("file") MultipartFile file,
                                 @RequestParam(defaultValue = "DEFAULT") String scene) {
        String url = fileService.uploadFile(file, scene);
        return Result.success(url);
    }
}