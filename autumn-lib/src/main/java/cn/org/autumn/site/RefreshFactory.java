package cn.org.autumn.site;

import cn.org.autumn.config.RefreshHandler;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RefreshFactory extends Factory {

    public void refresh() {
        List<RefreshHandler> handlers = getOrderList(RefreshHandler.class);
        RefreshHandler handler = null;
        if (null != handlers && handlers.size() > 0)
            handler = handlers.get(0);
        if (null == handler || handler.isRefresh())
            invoke(RefreshFactory.Refresh.class, "refresh");
    }

    public interface Refresh {
        @Order(DEFAULT_ORDER)
        void refresh();
    }
}
