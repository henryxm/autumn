package cn.org.autumn.site;

import cn.org.autumn.config.ClearHandler;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ClearFactory extends Factory {

    List<ClearHandler> list = null;

    public String clear() {
        if (null == list)
            list = getOrderList(ClearHandler.class);
        StringBuilder builder = new StringBuilder();
        for (ClearHandler handler : list) {
            handler.clear();
            Class<?> aClass = handler.getClass();
            String name = aClass.getSimpleName();
            String[] ar = name.split("\\$");
            builder.append(ar[0]);
            builder.append("<br/>");
        }
        return builder.toString();
    }
}
