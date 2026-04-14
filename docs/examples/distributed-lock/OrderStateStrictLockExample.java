package cn.org.autumn.examples.distributedlock;

import cn.org.autumn.base.ModuleService;
import org.springframework.stereotype.Service;

/**
 * 示例：订单状态推进三种加锁模式。
 * 1) strict: 强一致，锁失败直接抛错；
 * 2) fallback: 可降级，锁失败返回 false；
 * 3) retry: 热点冲突，锁失败短重试。
 */
@Service
public class OrderStateStrictLockExample extends ModuleService<OrderStateStrictLockExample.OrderDao, OrderStateStrictLockExample.OrderEntity> {

    /**
     * 默认入口：强一致模式。
     */
    public void updateOrderStatus(Long orderId, String nextStatus) throws Exception {
        updateOrderStatusStrict(orderId, nextStatus);
    }

    public void updateOrderStatusStrict(Long orderId, String nextStatus) throws Exception {
        final String lockKey = "order:status:" + orderId;
        withLock(lockKey, () -> {
            updateStatusInLock(orderId, nextStatus);
            return null;
        });
    }

    public boolean updateOrderStatusFallback(Long orderId, String nextStatus) {
        final String lockKey = "order:status:" + orderId;
        return withLockOrFallbackUnchecked(lockKey, () -> {
            updateStatusInLock(orderId, nextStatus);
            return true;
        }, (key, ex) -> {
            // 可降级场景：例如提示“稍后重试”
            return false;
        });
    }

    public boolean updateOrderStatusRetry(Long orderId, String nextStatus) {
        final String lockKey = "order:status:" + orderId;
        return withLockRetryUnchecked(lockKey, () -> {
            updateStatusInLock(orderId, nextStatus);
            return true;
        });
    }

    private void updateStatusInLock(Long orderId, String nextStatus) {
        OrderEntity order = baseMapper.selectById(orderId);
        if (order == null) {
            throw new IllegalStateException("order not found: " + orderId);
        }
        if (nextStatus.equals(order.getStatus())) {
            return;
        }
        order.setStatus(nextStatus);
        baseMapper.updateById(order);
    }

    /**
     * 以下内部类型只为示例自包含，实际项目请替换为真实 Dao/Entity。
     */
    public interface OrderDao extends com.baomidou.mybatisplus.mapper.BaseMapper<OrderEntity> {
    }

    public static class OrderEntity {
        private Long id;
        private String status;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }
}
