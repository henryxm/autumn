package cn.org.autumn.site;

import cn.org.autumn.search.SearchHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class SearchFactory extends Factory {

    List<SearchHandler> list = null;

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