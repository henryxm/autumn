package cn.org.autumn.site;

import cn.org.autumn.config.TableHandler;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TableFactory extends Factory {

    List<TableHandler> list = null;

    public String reinit() {
        if (null == list)
            list = getOrderList(TableHandler.class);
        StringBuilder builder = new StringBuilder();
        for (TableHandler handler : list) {
            handler.reinit();
            Class<?> aClass = handler.getClass();
            String name = aClass.getSimpleName();
            String[] ar = name.split("\\$");
            builder.append(ar[0]);
            builder.append("<br/>");
        }
        return builder.toString();
    }
}
