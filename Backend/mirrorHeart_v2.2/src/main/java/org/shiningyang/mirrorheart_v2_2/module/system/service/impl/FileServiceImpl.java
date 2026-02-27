package org.shiningyang.mirrorheart_v2_2.module.system.service.impl;

import cn.hutool.core.lang.UUID;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutObjectRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.shiningyang.mirrorheart_v2_2.common.exception.CustomException;
import org.shiningyang.mirrorheart_v2_2.config.CosConfig;
import org.shiningyang.mirrorheart_v2_2.module.system.entity.SysFile;
import org.shiningyang.mirrorheart_v2_2.module.system.mapper.SysFileMapper;
import org.shiningyang.mirrorheart_v2_2.module.system.service.IFileService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileServiceImpl extends ServiceImpl<SysFileMapper, SysFile> implements IFileService {

    @Value("${file.upload-path}")
    private String uploadPath;

    @Value("${file.access-path}")
    private String accessPath;

    private final COSClient cosClient;
    private final CosConfig cosConfig;

    @Override
    public String uploadFile(MultipartFile file, String directory) {
        if (file == null || file.isEmpty()) {
            throw new CustomException("上传文件不能为空");
        }

        // 1. 获取原始文件名和后缀
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        // 2. 生成唯一的文件名并按日期分片存储，防止单目录下文件过多
        // 格式：mirrorheart/avatar/2026/02/27/uuid.jpg
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String key = "mirrorheart/" + directory + "/" + datePath + "/" + uuid + extension;

        try {
            // 3. 构建元数据
            ObjectMetadata objectMetadata = new ObjectMetadata();
            objectMetadata.setContentLength(file.getSize());
            objectMetadata.setContentType(file.getContentType());

            // 4. 上传文件流
            InputStream inputStream = file.getInputStream();
            PutObjectRequest putObjectRequest = new PutObjectRequest(
                    cosConfig.getBucketName(),
                    key,
                    inputStream,
                    objectMetadata
            );
            cosClient.putObject(putObjectRequest);

            // 5. 拼接并返回完整的文件访问 URL
            return cosConfig.getBaseUrl() + "/" + key;

        } catch (Exception e) {
            log.error("文件上传至腾讯云COS失败: {}", e.getMessage(), e);
            throw new CustomException("文件上传失败，请稍后重试");
        }
    }
}