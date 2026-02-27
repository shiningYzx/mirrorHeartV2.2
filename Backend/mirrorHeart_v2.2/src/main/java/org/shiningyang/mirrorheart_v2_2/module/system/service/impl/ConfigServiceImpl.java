package org.shiningyang.mirrorheart_v2_2.module.system.service.impl;

import org.shiningyang.mirrorheart_v2_2.module.system.entity.Config;
import org.shiningyang.mirrorheart_v2_2.module.system.mapper.ConfigMapper;
import org.shiningyang.mirrorheart_v2_2.module.system.service.IConfigService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 系统配置表 服务实现类
 * </p>
 *
 * @author ShiningYang
 * @since 2026-02-18
 */
@Service
public class ConfigServiceImpl extends ServiceImpl<ConfigMapper, Config> implements IConfigService {

}
