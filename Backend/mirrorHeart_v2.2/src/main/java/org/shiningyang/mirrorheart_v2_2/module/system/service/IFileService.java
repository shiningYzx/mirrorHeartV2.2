package org.shiningyang.mirrorheart_v2_2.module.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.shiningyang.mirrorheart_v2_2.module.system.entity.SysFile;
import org.springframework.web.multipart.MultipartFile;

public interface IFileService extends IService<SysFile> {
    /**
     * 上传文件
     * @param file 文件对象
     * @param directory 场景 (AVATAR, POST_IMG, AUDIO)
     * @return 文件的完整访问 URL
     */
    String uploadFile(MultipartFile file, String directory);
}