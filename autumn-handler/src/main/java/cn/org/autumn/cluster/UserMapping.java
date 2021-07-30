package cn.org.autumn.cluster;

public interface UserMapping {
    String getUsername();

    String getUuid();

    default String getIdCard() {
        return "";
    }

    default String getMobile() {
        return "";
    }

    default String getEmail() {
        return "";
    }
}
