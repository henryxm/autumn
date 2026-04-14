package cn.org.autumn.modules.sys.entity;

import cn.org.autumn.annotation.ConfigField;
import cn.org.autumn.annotation.ConfigParam;
import cn.org.autumn.config.InputType;
import org.apache.commons.lang.StringUtils;

import java.io.Serializable;

@ConfigParam(paramKey = "LOADING_THEME", category = LoadingTheme.config, name = "加载页主题", description = "配置加载页的品牌、主色和图标")
public class LoadingTheme implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String config = "loading_theme_config";

    @ConfigField(category = InputType.StringType, name = "品牌名称", description = "加载页展示的品牌名称")
    private String brand = "Autumn Platform";

    @ConfigField(category = InputType.StringType, name = "主色", description = "加载页主题色（十六进制，例如#3c8dbc）")
    private String accent = "#3c8dbc";

    @ConfigField(category = InputType.StringType, name = "图标URL", description = "加载页品牌图标URL，支持以http://、https://或/开头")
    private String logoUrl = "";

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getAccent() {
        return accent;
    }

    public void setAccent(String accent) {
        this.accent = accent;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public void normalize() {
        if (StringUtils.isBlank(brand)) {
            brand = "Autumn Platform";
        } else {
            brand = brand.trim();
        }
        if (StringUtils.isBlank(accent)) {
            accent = "#3c8dbc";
        } else {
            accent = accent.trim();
            if (!accent.startsWith("#")) {
                accent = "#" + accent;
            }
            if (!accent.matches("^#[0-9a-fA-F]{6}$")) {
                accent = "#3c8dbc";
            }
        }
        if (StringUtils.isBlank(logoUrl)) {
            logoUrl = "";
        } else {
            logoUrl = logoUrl.trim();
            if (!(logoUrl.startsWith("http://") || logoUrl.startsWith("https://") || logoUrl.startsWith("/"))) {
                logoUrl = "";
            }
        }
    }
}
