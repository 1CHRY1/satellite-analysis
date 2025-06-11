package nnu.mnr.satellite.utils.dt;

import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Chry
 * @Date: 2024/12/12 17:13
 * @Description:
 */

@Component
@Slf4j
public class RedisUtil {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    public void removeValue(String key) {
        redisTemplate.opsForHash().delete(key);
    }

    // Time
    public boolean expire(String key, long time) {
        return redisTemplate.expire(key, time, TimeUnit.SECONDS);
    }
    public long getTime(String key) {
        return redisTemplate.getExpire(key, TimeUnit.SECONDS);
    }
    public boolean hasKey(String key) {
        return redisTemplate.hasKey(key);
    }
    public boolean persist(String key) {
        return redisTemplate.boundValueOps(key).persist();
    }

    // String Value
    public void addStringWithExpiration(String key, String value, int expiration) {
        redisTemplate.opsForValue().set(key, value);
        redisTemplate.expire(key, expiration, TimeUnit.SECONDS);
    }

    public String getStringData(String key) {
        return (String) redisTemplate.opsForValue().get(key);
    }

    public void removeStringData(String key) {
        redisTemplate.delete(key);
    }

    // HashKey Value
    public void addHashValue(String key, Map<String, String> map) {
        redisTemplate.opsForHash().putAll(key, map);
    }
    public Map<Object, Object> getHashEntries(String key) {
        return redisTemplate.opsForHash().entries(key);
    }
    public boolean hashKey(String key, String hashKey) {
        return redisTemplate.opsForHash().hasKey(key, hashKey);
    }
    public Long hashSize(String key) {
        return redisTemplate.opsForHash().size(key);
    }
    public void updateHashValue(String key, String hashKey, Object newValue) {
        redisTemplate.opsForHash().put(key, hashKey, newValue);
    }
    public void removeHashValue(String key, String hashKey) {
        redisTemplate.opsForHash().delete(key, hashKey);
    }

    public void addJsonDataWithExpiration(String key, JSONObject jsonObject, long expirationTime) {
        redisTemplate.opsForValue().set(key, jsonObject.toJSONString());
        redisTemplate.expire(key, expirationTime, TimeUnit.SECONDS);
    }

    public JSONObject getJsonData(String key) {
        String jsonString = (String) redisTemplate.opsForValue().get(key);
        return JSONObject.parseObject(jsonString);
    }

    public void updateJsonField(String key, String field, Object newValue) {
        try {
            String jsonString = (String) redisTemplate.opsForValue().get(key);
            JSONObject jsonObject = JSONObject.parseObject(jsonString);
            jsonObject.put(field, newValue);
            long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
            redisTemplate.opsForValue().set(key, jsonObject.toJSONString());
            if (ttl > 0) {
                redisTemplate.expire(key, ttl, TimeUnit.SECONDS);
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

}
