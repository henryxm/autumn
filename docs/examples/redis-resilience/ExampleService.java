// 文档示例：不位于 Maven 源码目录，不会被编译进 JAR。复制到业务模块（如 cn.org.autumn.modules.xxx）后再加 @Service。

// package your.app.service;
//
// import cn.org.autumn.redis.resilience.RedisResilience;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.data.redis.core.StringRedisTemplate;
// import org.springframework.stereotype.Service;
//
// import java.util.Optional;
// import java.util.concurrent.TimeUnit;
//
// @Service
// public class ExampleService {
//
//     @Autowired(required = false)
//     private RedisResilience redisResilience;
//
//     @Autowired(required = false)
//     private StringRedisTemplate stringRedisTemplate;
//
//     /** 读缓存：熔断或 Redis 故障时返回 empty，由调用方走 DB。 */
//     public Optional<String> getCachedTitle(String bizId) {
//         if (redisResilience == null || stringRedisTemplate == null) {
//             return Optional.empty();
//         }
//         try {
//             String v = redisResilience.execute(
//                     () -> stringRedisTemplate.opsForValue().get("demo:title:" + bizId),
//                     () -> null);
//             return Optional.ofNullable(v);
//         } catch (Exception e) {
//             return Optional.empty();
//         }
//     }
//
//     /** 写缓存：失败则吞掉，不影响主事务（仅示例）。 */
//     public void putCachedTitle(String bizId, String title) {
//         if (redisResilience == null || stringRedisTemplate == null) {
//             return;
//         }
//         try {
//             redisResilience.execute(() -> {
//                 stringRedisTemplate.opsForValue().set("demo:title:" + bizId, title, 10, TimeUnit.MINUTES);
//                 return null;
//             }, () -> null);
//         } catch (Exception ignored) {
//         }
//     }
// }
