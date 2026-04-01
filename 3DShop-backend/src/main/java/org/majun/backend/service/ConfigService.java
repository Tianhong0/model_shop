package org.majun.backend.service;

import org.majun.backend.dto.ConfigUpdateRequest;
import org.majun.backend.vo.ConfigVO;

import java.util.List;
import java.util.Map;

/**
 * 系统配置服务接口
 */
public interface ConfigService {

    /**
     * 获取配置值
     */
    String getConfigValue(String key);

    /**
     * 获取配置值，带默认值
     */
    String getConfigValue(String key, String defaultValue);

    /**
     * 设置配置
     */
    void setConfig(ConfigUpdateRequest request);

    /**
     * 批量设置配置
     */
    void setConfigs(Map<String, String> configs);

    /**
     * 删除配置
     */
    void deleteConfig(String key);

    /**
     * 获取配置详情
     */
    ConfigVO getConfigDetail(String key);

    /**
     * 获取所有配置
     */
    List<ConfigVO> getAllConfigs();

    /**
     * 获取指定分组的配置
     */
    List<ConfigVO> getConfigsByGroup(String group);

    /**
     * 获取公开配置（无需权限）
     */
    Map<String, String> getPublicConfigs();
}
