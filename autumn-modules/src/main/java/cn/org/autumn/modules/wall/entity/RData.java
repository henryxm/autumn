package cn.org.autumn.modules.wall.entity;

public class RData {
    String host = "";
    String ip = "";
    String userAgent = "";
    String uri = "";
    String refer = "";
    int count;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getIp() {
        if (null == ip)
            ip = "";
        return ip;
    }

    public void setIp(String ip) {
        if (null == ip)
            ip = "";
        this.ip = ip;
    }

    public String getUserAgent() {
        if (null == userAgent)
            userAgent = "";
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        if (null == userAgent)
            userAgent = "";
        this.userAgent = userAgent;
    }

    public String getUri() {
        if (null == uri)
            uri = "";
        return uri;
    }

    public void setUri(String uri) {
        if (null == uri)
            uri = "";
        this.uri = uri;
    }

    public String getRefer() {
        if (null == refer)
            refer = "";
        return refer;
    }

    public void setRefer(String refer) {
        if (null == refer)
            refer = "";
        this.refer = refer;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    @Override
    public String toString() {
        return "RData{" +
                "host='" + host + '\'' +
                ", ip='" + ip + '\'' +
                ", userAgent='" + userAgent + '\'' +
                ", uri='" + uri + '\'' +
                ", refer='" + refer + '\'' +
                ", count=" + count +
                '}';
    }
}
