package cn.org.autumn.modules.spm.entity;

public interface Spm {

    String getSiteId();

    String getPageId();

    String getChannelId();

    String getProductId();

    String getResourceId();

    String getUrlPath();

    String getUrlKey();

    String getSpmValue();

    /**
     * spm split with dot for index
     *
     * @param i zero based
     * @return index string
     */
    String indexOf(int i);

    boolean forbidden();

    boolean needLogin();
}