package org.majun.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.majun.backend.dto.ConfigUpdateRequest;
import org.majun.backend.entity.SysConfig;
import org.majun.backend.repository.SysConfigRepository;
import org.majun.backend.service.ConfigService;
import org.majun.backend.vo.ConfigVO;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 系统配置服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
/**
 * 配置服务实现
 */
public class ConfigServiceImpl implements ConfigService {

    private final SysConfigRepository configRepository;

    @Override
    public String getConfigValue(String key) {
        return getConfigValue(key, null);
    }

    @Override
    public String getConfigValue(String key, String defaultValue) {
        LambdaQueryWrapper<SysConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysConfig::getConfigKey, key);
        SysConfig config = configRepository.selectOne(wrapper);
        if (config != null && config.getConfigValue() != null) {
            return config.getConfigValue();
        }
        return defaultValue;
    }

    @Override
    public void setConfig(ConfigUpdateRequest request) {
        LambdaQueryWrapper<SysConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysConfig::getConfigKey, request.getConfigKey());
        SysConfig existing = configRepository.selectOne(wrapper);

        if (existing != null) {
            existing.setConfigValue(request.getConfigValue());
            if (request.getConfigType() != null) {
                existing.setConfigType(request.getConfigType());
            }
            if (request.getConfigGroup() != null) {
                existing.setConfigGroup(request.getConfigGroup());
            }
            if (request.getDescription() != null) {
                existing.setDescription(request.getDescription());
            }
            if (request.getIsPublic() != null) {
                existing.setIsPublic(request.getIsPublic());
            }
            configRepository.updateById(existing);
        } else {
            SysConfig config = new SysConfig();
            config.setConfigKey(request.getConfigKey());
            config.setConfigValue(request.getConfigValue());
            config.setConfigType(request.getConfigType() != null ? request.getConfigType() : "STRING");
            config.setConfigGroup(request.getConfigGroup() != null ? request.getConfigGroup() : "SYSTEM");
            config.setDescription(request.getDescription());
            config.setIsPublic(request.getIsPublic() != null ? request.getIsPublic() : 0);
            configRepository.insert(config);
        }
    }

    @Override
    public void setConfigs(Map<String, String> configs) {
        for (Map.Entry<String, String> entry : configs.entrySet()) {
            ConfigUpdateRequest request = new ConfigUpdateRequest();
            request.setConfigKey(entry.getKey());
            request.setConfigValue(entry.getValue());
            setConfig(request);
        }
    }

    @Override
    public void deleteConfig(String key) {
        LambdaQueryWrapper<SysConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysConfig::getConfigKey, key);
        configRepository.delete(wrapper);
    }

    @Override
    public ConfigVO getConfigDetail(String key) {
        LambdaQueryWrapper<SysConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysConfig::getConfigKey, key);
        SysConfig config = configRepository.selectOne(wrapper);
        return config != null ? toConfigVO(config) : null;
    }

    @Override
    public List<ConfigVO> getAllConfigs() {
        LambdaQueryWrapper<SysConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(SysConfig::getConfigGroup)
                .orderByAsc(SysConfig::getConfigKey);
        return configRepository.selectList(wrapper).stream()
                .map(this::toConfigVO)
                .toList();
    }

    @Override
    public List<ConfigVO> getConfigsByGroup(String group) {
        LambdaQueryWrapper<SysConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysConfig::getConfigGroup, group)
                .orderByAsc(SysConfig::getConfigKey);
        return configRepository.selectList(wrapper).stream()
                .map(this::toConfigVO)
                .toList();
    }

    @Override
    public Map<String, String> getPublicConfigs() {
        LambdaQueryWrapper<SysConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysConfig::getIsPublic, 1);
        List<SysConfig> configs = configRepository.selectList(wrapper);
        Map<String, String> result = new HashMap<>();
        for (SysConfig config : configs) {
            result.put(config.getConfigKey(), config.getConfigValue());
        }
        return result;
    }

    private ConfigVO toConfigVO(SysConfig config) {
        ConfigVO vo = new ConfigVO();
        vo.setId(config.getId());
        vo.setConfigKey(config.getConfigKey());
        vo.setConfigValue(config.getConfigValue());
        vo.setConfigType(config.getConfigType());
        vo.setConfigGroup(config.getConfigGroup());
        vo.setDescription(config.getDescription());
        vo.setIsPublic(config.getIsPublic());
        vo.setCreateTime(config.getCreateTime());
        vo.setUpdateTime(config.getUpdateTime());
        return vo;
    }
}
