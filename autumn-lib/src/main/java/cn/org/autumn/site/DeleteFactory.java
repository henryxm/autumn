package cn.org.autumn.site;

import cn.org.autumn.config.DeleteHandler;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 数据清理工厂
 */
@Component
public class DeleteFactory extends Factory implements DeleteHandler {

    List<DeleteHandler> list = null;

    /**
     * 计算对象是否可以删除，如果有一个实例返回不能删除，则不能删除
     * 如果没有实例领，则可以删除
     *
     * @param obj 需要删除的对象
     * @return true可以删除，false不可以删除
     */
    public boolean deletable(Object obj) {
        if (null == list)
            list = getOrderList(DeleteHandler.class);
        for (DeleteHandler handler : list) {
            if (handler instanceof DeleteFactory)
                continue;
            boolean deletable = handler.deletable(obj);
            if (!deletable)
                return false;
        }
        return true;
    }
}
