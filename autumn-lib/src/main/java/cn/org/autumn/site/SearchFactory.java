package cn.org.autumn.site;

import cn.org.autumn.search.IType;
import cn.org.autumn.search.SearchHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class SearchFactory extends Factory {

    List<SearchHandler> list = null;

    public List<IType> types() {
        if (null == list)
            list = getOrderList(SearchHandler.class);
        List<IType> results = new ArrayList<>();
        for (SearchHandler handler : list) {
            if (null != handler.types() && !handler.types().isEmpty())
                results.addAll(handler.types());
        }
        return results;
    }

    public List<Object> search(Object value) {
        if (null == list)
            list = getOrderList(SearchHandler.class);
        List<Object> results = new ArrayList<>();
        for (SearchHandler handler : list) {
            Object result = handler.search(value);
            if (null != result)
                results.add(result);
        }
        return results;
    }
}