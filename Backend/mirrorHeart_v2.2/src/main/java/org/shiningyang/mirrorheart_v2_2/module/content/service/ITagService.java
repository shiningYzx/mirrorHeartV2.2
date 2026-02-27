package org.shiningyang.mirrorheart_v2_2.module.content.service;

import org.shiningyang.mirrorheart_v2_2.module.content.entity.Tag;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 * 标签库 服务类
 * </p>
 *
 * @author ShiningYang
 * @since 2026-02-18
 */
public interface ITagService extends IService<Tag> {
    /**
     * 按分组获取标签列表 (走 Redis 缓存)
     */
    List<Tag> getTagsByGroup(String group);

    /**
     * 新增标签 (清除缓存)
     */
    void addTag(Tag tag);
}
