package cn.org.autumn.site;

import cn.org.autumn.search.IType;
import cn.org.autumn.search.SearchHandler;
import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SearchFactory extends Factory {

    @Autowired
    Gson gson;

    List<SearchHandler> list = null;

    public List<IType> types() {
        if (null == list)
            list = getOrderList(SearchHandler.class);
        List<IType> results = new ArrayList<>();
        for (SearchHandler handler : list) {
            try {
                if (null != handler.types() && !handler.types().isEmpty())
                    results.addAll(handler.types());
            } catch (Exception e) {
                log.error("Type error: {}, error: {}", handler.getClass().getSimpleName(), e.getMessage());
            }
        }
        return results;
    }

    public List<Object> search(Object value) {
        if (null == list)
            list = getOrderList(SearchHandler.class);
        List<Object> results = new ArrayList<>();
        for (SearchHandler handler : list) {
            try {
                Object result = handler.search(value);
                if (null != result)
                    results.add(result);
            } catch (Exception e) {
                log.error("Search error: {}, request: {}, error: {}", handler.getClass().getSimpleName(), gson.toJson(value), e.getMessage());
            }
        }
        return results;
    }
}
