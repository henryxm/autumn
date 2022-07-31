package cn.org.autumn.plugin;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

public class PluginEntry implements Serializable {
    private static final long serialVersionUID = 1L;

    private String uuid;

    private String name;

    private String author;

    private String namespace;

    private String url;

    private String logo;

    private String banner;

    private String index;

    //设为主要启动插件，优先使用该插件的index首页
    private Boolean main;

    private String price;

    private String version;

    private Object plugin;

    private List<String> images;

    private String summary;

    private String description;

    private Integer code;

    private String msg;

    private String data;

    private String file;

    private Date createTime;

    private Date updateTime;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getLogo() {
        return logo;
    }

    public void setLogo(String logo) {
        this.logo = logo;
    }

    public String getBanner() {
        return banner;
    }

    public void setBanner(String banner) {
        this.banner = banner;
    }

    public String getIndex() {
        return index;
    }

    public void setIndex(String index) {
        this.index = index;
    }

    public Boolean getMain() {
        return main;
    }

    public void setMain(Boolean main) {
        this.main = main;
    }

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Object getPlugin() {
        return plugin;
    }

    public void setPlugin(Object plugin) {
        this.plugin = plugin;
    }

    public List<String> getImages() {
        return images;
    }

    public void setImages(List<String> images) {
        this.images = images;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public static boolean isBlank(String str) {
        int strLen;
        if (str != null && (strLen = str.length()) != 0) {
            for (int i = 0; i < strLen; ++i) {
                if (!Character.isWhitespace(str.charAt(i))) {
                    return false;
                }
            }
            return true;
        } else {
            return true;
        }
    }

    public void merge(PluginEntry pluginEntry) {
        if (null != pluginEntry) {
            if (!isBlank(pluginEntry.getIndex()))
                setIndex(pluginEntry.getIndex());
        }
    }

    public PluginEntry copy() {
        PluginEntry pluginEntry = new PluginEntry();
        pluginEntry.setIndex(index);
        pluginEntry.setCode(code);
        pluginEntry.setMsg(msg);
        pluginEntry.setData(data);
        pluginEntry.setDescription(description);
        pluginEntry.setNamespace(namespace);
        pluginEntry.setUuid(uuid);
        pluginEntry.setAuthor(author);
        pluginEntry.setBanner(banner);
        pluginEntry.setCreateTime(createTime);
        pluginEntry.setUpdateTime(updateTime);
        pluginEntry.setUrl(url);
        pluginEntry.setImages(images);
        pluginEntry.setMain(main);
        pluginEntry.setLogo(logo);
        pluginEntry.setSummary(summary);
        pluginEntry.setPrice(price);
        pluginEntry.setName(name);
        pluginEntry.setVersion(version);
        return pluginEntry;
    }
}