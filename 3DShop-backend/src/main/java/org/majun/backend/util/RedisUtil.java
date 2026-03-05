package org.majun.backend.util;

import lombok.extern.slf4j.Slf4j;
import org.majun.backend.entity.SysRole;
import org.majun.backend.entity.SysUser;
import org.majun.backend.security.LoginUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Redis 工具类
 */
@Slf4j
@Component
public class RedisUtil {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 缓存过期时间（秒）
     */
    public static final long CACHE_EXPIRE_SECONDS = 3600; // 1小时

    /**
     * Token 过期时间（秒）
     */
    private static final long TOKEN_EXPIRE_SECONDS = 604800; // 7天

    /**
     * Token key 前缀
     */
    private static final String TOKEN_KEY_PREFIX = "token:";

    /**
     * 模型列表缓存 key 前缀
     */
    private static final String MODEL_LIST_KEY_PREFIX = "model:list:";

    /**
     * 模型详情缓存 key 前缀
     */
    private static final String MODEL_DETAIL_KEY_PREFIX = "model:detail:";

    /**
     * 分类列表缓存 key 前缀
     */
    private static final String CATEGORY_LIST_KEY_PREFIX = "category:list:";

    /**
     * 分类详情缓存 key 前缀
     */
    private static final String CATEGORY_TREE_KEY_PREFIX = "category:tree:";

    /**
     * 用户当前Token映射 key 前缀（反向索引：userId → token）
     */
    private static final String USER_TOKEN_KEY_PREFIX = "user:token:";

    // ==================== Token 操作 ====================

    /**
     * 将 Token 存入 Redis，同时建立 userId → token 的反向映射
     */
    public void setToken(String token, Long userId) {
        // 先删除该用户的旧 token
        deleteOldToken(userId);

        // 清理历史遗留的“裸token作为key”写法（兼容旧版本）
        stringRedisTemplate.delete(token);

        // 存新 token
        String key = TOKEN_KEY_PREFIX + token;
        stringRedisTemplate.opsForValue().set(key, String.valueOf(userId), TOKEN_EXPIRE_SECONDS, TimeUnit.SECONDS);

        // 建立反向映射 user:token:{userId} → token
        String userTokenKey = USER_TOKEN_KEY_PREFIX + userId;
        stringRedisTemplate.opsForValue().set(userTokenKey, token, TOKEN_EXPIRE_SECONDS, TimeUnit.SECONDS);

        log.info("Token 存入 Redis, userId: {}", userId);
    }

    /**
     * 删除用户的旧 Token（重复登录时调用）
     */
    private void deleteOldToken(Long userId) {
        String userTokenKey = USER_TOKEN_KEY_PREFIX + userId;
        String oldToken = stringRedisTemplate.opsForValue().get(userTokenKey);
        if (oldToken != null) {
            // 删除旧的 token:{token} 键
            stringRedisTemplate.delete(TOKEN_KEY_PREFIX + oldToken);
            // 删除反向映射
            stringRedisTemplate.delete(userTokenKey);
            log.info("用户 {} 的旧Token已失效", userId);
        }
    }

    /**
     * 检查 Token 是否有效（存在于 Redis 中）
     */
    public boolean isTokenValid(String token) {
        String key = TOKEN_KEY_PREFIX + token;
        return stringRedisTemplate.hasKey(key);
    }

    /**
     * 严格校验 Token：要求 token:{token} 和 user:token:{userId} 双向一致
     */
    public boolean isTokenValid(String token, Long userId) {
        if (token == null || userId == null) {
            return false;
        }

        String tokenKey = TOKEN_KEY_PREFIX + token;
        String userTokenKey = USER_TOKEN_KEY_PREFIX + userId;

        String userIdInTokenKey = stringRedisTemplate.opsForValue().get(tokenKey);
        String tokenInUserKey = stringRedisTemplate.opsForValue().get(userTokenKey);

        return String.valueOf(userId).equals(userIdInTokenKey) && token.equals(tokenInUserKey);
    }

    /**
     * 删除 Token（退出登录时），同时清理反向映射
     */
    public void deleteToken(String token) {
        String key = TOKEN_KEY_PREFIX + token;
        // 获取 userId 用于清理反向映射
        String userId = stringRedisTemplate.opsForValue().get(key);
        if (userId != null) {
            stringRedisTemplate.delete(USER_TOKEN_KEY_PREFIX + userId);
        }
        // 清理历史遗留的“裸token作为key”写法
        stringRedisTemplate.delete(token);
        stringRedisTemplate.delete(key);
        log.info("Token 已从 Redis 删除");
    }

    /**
     * 按用户ID删除当前有效Token（用于改密后强制下线）
     */
    public void deleteTokenByUserId(Long userId) {
        if (userId == null) {
            return;
        }
        String userTokenKey = USER_TOKEN_KEY_PREFIX + userId;
        String token = stringRedisTemplate.opsForValue().get(userTokenKey);
        if (token != null) {
            stringRedisTemplate.delete(TOKEN_KEY_PREFIX + token);
        }
        stringRedisTemplate.delete(userTokenKey);
        log.info("用户 {} 的登录Token已失效", userId);
    }

    // ==================== 模型缓存操作 ====================

    /**
     * 缓存模型列表
     */
    public void setModelList(String cacheKey, String data) {
        String key = MODEL_LIST_KEY_PREFIX + cacheKey;
        stringRedisTemplate.opsForValue().set(key, data, CACHE_EXPIRE_SECONDS, TimeUnit.SECONDS);
        log.debug("模型列表已缓存, key: {}", key);
    }

    /**
     * 获取缓存的模型列表
     */
    public String getModelList(String cacheKey) {
        String key = MODEL_LIST_KEY_PREFIX + cacheKey;
        String data = stringRedisTemplate.opsForValue().get(key);
        if (data != null) {
            log.debug("模型列表缓存命中, key: {}", key);
        } else {
            log.debug("模型列表缓存未命中, key: {}", key);
        }
        return data;
    }

    /**
     * 删除模型列表缓存
     */
    public void deleteModelList(String cacheKey) {
        String key = MODEL_LIST_KEY_PREFIX + cacheKey;
        stringRedisTemplate.delete(key);
        log.debug("模型列表缓存已删除, key: {}", key);
    }

    /**
     * 删除所有模型列表缓存（模糊匹配）
     */
    public void deleteAllModelLists() {
        String pattern = MODEL_LIST_KEY_PREFIX + "*";
        stringRedisTemplate.delete(stringRedisTemplate.keys(pattern));
        log.info("所有模型列表缓存已清除");
    }

    // ==================== 模型详情缓存操作 ====================

    /**
     * 缓存模型详情
     */
    public void setModelDetail(Long modelId, String data) {
        String key = MODEL_DETAIL_KEY_PREFIX + modelId;
        stringRedisTemplate.opsForValue().set(key, data, CACHE_EXPIRE_SECONDS, TimeUnit.SECONDS);
        log.debug("模型详情已缓存, key: {}", key);
    }

    /**
     * 获取缓存的模型详情
     */
    public String getModelDetail(Long modelId) {
        String key = MODEL_DETAIL_KEY_PREFIX + modelId;
        String data = stringRedisTemplate.opsForValue().get(key);
        if (data != null) {
            log.debug("模型详情缓存命中, key: {}", key);
        } else {
            log.debug("模型详情缓存未命中, key: {}", key);
        }
        return data;
    }

    /**
     * 删除模型详情缓存
     */
    public void deleteModelDetail(Long modelId) {
        String key = MODEL_DETAIL_KEY_PREFIX + modelId;
        stringRedisTemplate.delete(key);
        log.debug("模型详情缓存已删除, key: {}", key);
    }

    // ==================== 分类缓存操作 ====================

    /**
     * 缓存分类树
     */
    public void setCategoryTree(String cacheKey, String data) {
        String key = CATEGORY_TREE_KEY_PREFIX + cacheKey;
        stringRedisTemplate.opsForValue().set(key, data, CACHE_EXPIRE_SECONDS, TimeUnit.SECONDS);
        log.debug("分类树已缓存, key: {}", key);
    }

    /**
     * 获取缓存的分类树
     */
    public String getCategoryTree(String cacheKey) {
        String key = CATEGORY_TREE_KEY_PREFIX + cacheKey;
        String data = stringRedisTemplate.opsForValue().get(key);
        if (data != null) {
            log.debug("分类树缓存命中, key: {}", key);
        } else {
            log.debug("分类树缓存未命中, key: {}", key);
        }
        return data;
    }

    /**
     * 删除分类树缓存
     */
    public void deleteCategoryTree(String cacheKey) {
        String key = CATEGORY_TREE_KEY_PREFIX + cacheKey;
        stringRedisTemplate.delete(key);
        log.debug("分类树缓存已删除, key: {}", key);
    }

    /**
     * 删除所有分类缓存
     */
    public void deleteAllCategories() {
        String pattern = CATEGORY_LIST_KEY_PREFIX + "*";
        stringRedisTemplate.delete(stringRedisTemplate.keys(pattern));
        pattern = CATEGORY_TREE_KEY_PREFIX + "*";
        stringRedisTemplate.delete(stringRedisTemplate.keys(pattern));
        log.info("所有分类缓存已清除");
    }


    /**
     * 设置模型列表缓存
     */
    public void setModelList(String cacheKey, String data, long expireSeconds) {
        stringRedisTemplate.opsForValue().set(cacheKey, data, expireSeconds, TimeUnit.SECONDS);
        log.debug("模型列表已缓存, key: {}", cacheKey);
    }



    /**
     * 设置分类树缓存
     */
    public void setCategoryTree(String cacheKey, String data, long expireSeconds) {
        stringRedisTemplate.opsForValue().set(cacheKey, data, expireSeconds, TimeUnit.SECONDS);
        log.debug("分类树已缓存, key: {}", cacheKey);
    }

    // ==================== 用户详情缓存操作 ====================

    /**
     * 缓存登录用户信息（包含角色）
     */
    public void setLoginUser(String cacheKey, LoginUser loginUser, long expireSeconds) {
        // 格式: id:userName:nickname:role1,role2,role3
        StringBuilder value = new StringBuilder();
        value.append(loginUser.getUser().getId())
             .append(":")
             .append(loginUser.getUser().getUserName())
             .append(":")
             .append(loginUser.getUser().getNickname())
             .append(":");
        if (loginUser.getRoles() != null && !loginUser.getRoles().isEmpty()) {
            List<String> roleNames = loginUser.getRoles().stream()
                    .map(SysRole::getRoleName)
                    .toList();
            value.append(String.join(",", roleNames));
        }
        stringRedisTemplate.opsForValue().set(cacheKey, value.toString(), expireSeconds, TimeUnit.SECONDS);
        log.debug("用户详情已缓存, key: {}", cacheKey);
    }

    /**
     * 获取缓存的登录用户（包含角色）
     */
    public LoginUser getLoginUser(String cacheKey) {
        String value = stringRedisTemplate.opsForValue().get(cacheKey);
        if (value != null) {
            log.debug("用户详情缓存命中, key: {}", cacheKey);
            String[] parts = value.split(":", 4);
            if (parts.length >= 3) {
                SysUser user = new SysUser();
                user.setId(Long.parseLong(parts[0]));
                user.setUserName(parts[1]);
                user.setNickname(parts[2]);

                // 恢复角色信息
                List<SysRole> roles = new ArrayList<>();
                if (parts.length == 4 && !parts[3].isEmpty()) {
                    String[] roleNames = parts[3].split(",");
                    for (String roleName : roleNames) {
                        SysRole role = new SysRole();
                        role.setRoleName(roleName);
                        role.setStatus(1);
                        roles.add(role);
                    }
                }

                return new LoginUser(user, roles);
            }
        }
        log.debug("用户详情缓存未命中, key: {}", cacheKey);
        return null;
    }

    /**
     * 删除登录用户缓存
     */
    public void deleteLoginUser(String cacheKey) {
        stringRedisTemplate.delete(cacheKey);
        log.debug("用户详情缓存已删除, key: {}", cacheKey);
    }

    // ==================== 通用键值操作 ====================

    public void setString(String key, String value, long expireSeconds) {
        stringRedisTemplate.opsForValue().set(key, value, expireSeconds, TimeUnit.SECONDS);
    }

    public String getString(String key) {
        return stringRedisTemplate.opsForValue().get(key);
    }

    public void deleteKey(String key) {
        stringRedisTemplate.delete(key);
    }

    public boolean hasKey(String key) {
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(key));
    }

    public long increment(String key, long delta) {
        Long value = stringRedisTemplate.opsForValue().increment(key, delta);
        return value == null ? 0L : value;
    }

    public void expire(String key, long seconds) {
        stringRedisTemplate.expire(key, seconds, TimeUnit.SECONDS);
    }
}
