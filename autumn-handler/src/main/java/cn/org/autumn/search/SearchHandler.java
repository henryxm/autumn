package cn.org.autumn.search;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConditionalOnMissingBean(SearchHandler.class)
public interface SearchHandler {
    List<IType> types();

    default Object search(Object value) {
        if (value instanceof IPage) {
            try {
                IPage<?> page = (IPage<?>) value;
                if (page.getData() instanceof ISearch) {
                    return search((IPage<ISearch>) page);
                }
            } catch (Exception ignored) {
            }
        } else if (value instanceof ISearch) {
            return search((ISearch) value);
        }
        return null;
    }

    default boolean can(List<String> types, Class<?> clazz) {
        return null == types || types.isEmpty() || types.contains(ISearch.getType(clazz));
    }

    default Object search(IPage<ISearch> value) {
        if (null != value)
            return search(value.getData());
        return null;
    }

    default Object search(ISearch search) {
        if (null != search)
            return search(search.getTypes(), search.getText());
        return null;
    }

    default Object search(String text) {
        return null;
    }

    default Object search(List<String> types, String text) {
        if (null != text)
            return search(text);
        return null;
    }
}
