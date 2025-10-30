package cn.org.autumn.model;

import cn.org.autumn.annotation.SearchType;
import cn.org.autumn.search.IType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SearchTypeValue implements IType {
    String type;
    String name;
    String alias;
    String describe;
    int order = 0;
    boolean show = true;

    public SearchTypeValue(Class<?> clazz) {
        if (null != clazz) {
            SearchType searchType = clazz.getDeclaredAnnotation(SearchType.class);
            if (null != searchType) {
                this.type = searchType.value();
                this.name = searchType.name();
                this.alias = searchType.alias();
                this.describe = searchType.describe();
                this.show = searchType.show();
                if (StringUtils.isBlank(this.type))
                    this.type = clazz.getSimpleName();
            }
        }
    }

    public static List<IType> of(Class<?>... classes) {
        List<IType> list = new ArrayList<>();
        for (Class<?> clazz : classes) {
            SearchTypeValue value = new SearchTypeValue(clazz);
            list.add(value);
        }
        return list;
    }
}
