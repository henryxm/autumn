package cn.org.autumn.config;

import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnMissingBean(CategoryHandler.class)
public interface CategoryHandler {
    String category_lang_string = "category_lang_string_";
    String config_lang_string = "config_lang_string_";

    default String[][] getCategoryItems() {
        return null;
    }

    default String categoryName(String category) {
        if (StringUtils.isBlank(category))
            return "";
        return category_lang_string + category.toLowerCase() + "_name";
    }

    default String categoryDescription(String category) {
        if (StringUtils.isBlank(category))
            return "";
        return category_lang_string + category.toLowerCase() + "_description";
    }

    default String configName(String config) {
        if (StringUtils.isBlank(config))
            return "";
        return config_lang_string + config.toLowerCase() + "_name";
    }

    default String configDescription(String config) {
        if (StringUtils.isBlank(config))
            return "";
        return config_lang_string + config.toLowerCase() + "_description";
    }
}
