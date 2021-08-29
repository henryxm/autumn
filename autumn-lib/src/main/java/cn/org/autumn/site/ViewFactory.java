package cn.org.autumn.site;

import cn.org.autumn.config.ViewHandler;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ViewFactory extends Factory {
    List<ViewHandler> viewHandlers = null;

    public ViewHandler getShould(String viewName) {
        if (null == viewHandlers)
            viewHandlers = getOrderList(ViewHandler.class);
        if (null != viewHandlers && !viewHandlers.isEmpty()) {
            for (ViewHandler viewHandler : viewHandlers) {
                if (viewHandler.should(viewName))
                    return viewHandler;
            }
        }
        return null;
    }
}