package cn.org.autumn.site;

import cn.org.autumn.config.DeleteHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 数据清理工厂
 */
@Slf4j
@Component
public class DeleteFactory extends Factory implements DeleteHandler {

    List<DeleteHandler> list = null;

    /**
     * 计算对象是否可以删除：任一 Handler 返回不可删、抛异常，或无 Handler 时，均不可删（保守策略）。
     *
     * @param obj 需要删除的对象
     * @return true 可以删除，false 不可以删除
     */
    public boolean deletable(Object obj) {
        if (null == list)
            list = getOrderList(DeleteHandler.class);
        if (null == list || list.isEmpty()) {
            return false;
        }
        for (DeleteHandler handler : list) {
            if (handler instanceof DeleteFactory)
                continue;
            try {
                if (!handler.deletable(obj)) {
                    return false;
                }
            } catch (Exception e) {
                log.warn("删除异常，保守跳过: handler={}, 错误: {}", handler.getClass().getSimpleName(), e.getMessage());
                return false;
            }
        }
        return true;
    }
}
