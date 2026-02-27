package org.shiningyang.mirrorheart_v2_2.module.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.shiningyang.mirrorheart_v2_2.module.content.entity.Tag;
import org.shiningyang.mirrorheart_v2_2.module.content.mapper.TagMapper;
import org.shiningyang.mirrorheart_v2_2.module.content.service.ITagService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TagServiceImpl extends ServiceImpl<TagMapper, Tag> implements ITagService {

    @Override
    @Cacheable(value = "tags:group", key = "#group != null ? #group : 'all'")
    public List<Tag> getTagsByGroup(String group) {
        return this.list(new LambdaQueryWrapper<Tag>()
                .eq(group != null, Tag::getGroupName, group)
                .orderByDesc(Tag::getUseCount));
    }

    @Override
    @CacheEvict(value = "tags:group", allEntries = true) // 任何新增都会清空所有标签缓存，保证下次查询是最新的
    public void addTag(Tag tag) {
        this.save(tag);
    }
}