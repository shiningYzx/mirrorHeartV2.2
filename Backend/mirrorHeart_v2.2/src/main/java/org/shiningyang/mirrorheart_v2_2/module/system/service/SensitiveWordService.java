package org.shiningyang.mirrorheart_v2_2.module.system.service;

import cn.hutool.dfa.WordTree;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.shiningyang.mirrorheart_v2_2.common.exception.CustomException;
import org.shiningyang.mirrorheart_v2_2.module.system.entity.SensitiveWord;
import org.shiningyang.mirrorheart_v2_2.module.system.mapper.SensitiveWordMapper;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor // [新增] 用于自动注入 Mapper
public class SensitiveWordService {

    // Hutool 提供的 DFA 算法敏感词树，性能极高
    private final WordTree wordTree = new WordTree();
    private final SensitiveWordMapper sensitiveWordMapper; // [新增] 注入敏感词 Mapper

    /**
     * 项目启动时自动初始化加载
     */
    @PostConstruct
    public void init() {
        refreshSensitiveWords();
    }

    /**
     * [重构] 从数据库重新加载敏感词库
     * (当管理员在后台增删敏感词后，Controller 中可直接调用此方法刷新 DFA 树，无需重启服务)
     */
    public void refreshSensitiveWords() {
        try {
            // 1. 清空旧的词树缓存
            wordTree.clear();

            // 2. 从数据库查询所有敏感词
            List<SensitiveWord> list = sensitiveWordMapper.selectList(null);

            if (list != null && !list.isEmpty()) {
                // 3. 提取文字列表 (注意：假设你的数据库字段名叫 word，如果叫 text 请改为 getText())
                List<String> words = list.stream()
                        .map(SensitiveWord::getWord)
                        .filter(w -> w != null && !w.trim().isEmpty())
                        .collect(Collectors.toList());

                // 4. 重载入 Hutool DFA 词树
                wordTree.addWords(words);
                log.info("敏感词库加载完成，共从数据库加载 {} 个词汇", words.size());
            } else {
                log.warn("敏感词库为空，未从数据库加载到任何词汇");
            }
        } catch (Exception e) {
            log.error("初始化敏感词库失败", e);
        }
    }

    /**
     * 检查文本是否包含敏感词
     * @param text 待检查文本
     * @return 包含返回 true，否则返回 false
     */
    public boolean containsSensitive(String text) {
        if (text == null || text.trim().isEmpty()) {
            return false;
        }
        return wordTree.isMatch(text);
    }

    /**
     * 查找文本中包含的第一个敏感词（用于提示用户）
     * @param text 待检查文本
     * @return 敏感词，没有则返回 null
     */
    public String getFirstMatchWord(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        return wordTree.match(text);
    }

    // ==========================================
    // 下方为新增的管理员操作 API 支持逻辑
    // ==========================================

    /**
     * [管理端] 分页获取敏感词
     */
    public IPage<SensitiveWord> getWordPage(Page<SensitiveWord> page, String keyword) {
        return sensitiveWordMapper.selectPage(page, new LambdaQueryWrapper<SensitiveWord>()
                .like(keyword != null && !keyword.isEmpty(), SensitiveWord::getWord, keyword)
                .orderByDesc(SensitiveWord::getId)
        );
    }

    /**
     * [管理端] 添加敏感词并刷新缓存
     */
    public void addWord(String word) {
        if (word == null || word.trim().isEmpty()) {
            throw new CustomException("敏感词不能为空");
        }
        String cleanWord = word.trim();

        // 校验是否重复
        boolean exists = sensitiveWordMapper.exists(new LambdaQueryWrapper<SensitiveWord>()
                .eq(SensitiveWord::getWord, cleanWord));
        if (exists) {
            throw new CustomException("该敏感词已存在库中");
        }

        SensitiveWord sw = new SensitiveWord();
        sw.setWord(cleanWord);
        sensitiveWordMapper.insert(sw);

        // 实时刷新 DFA 树
        refreshSensitiveWords();
    }

    /**
     * [管理端] 删除敏感词并刷新缓存
     */
    public void deleteWord(Long id) {
        int rows = sensitiveWordMapper.deleteById(id);
        if (rows > 0) {
            // 实时刷新 DFA 树
            refreshSensitiveWords();
        }
    }
}