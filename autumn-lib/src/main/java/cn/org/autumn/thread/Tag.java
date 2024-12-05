package cn.org.autumn.thread;

import java.util.Date;

public interface Tag {

    String getId();

    String getName();

    void setName(String name);

    Date getTime();

    void setTime(Date time);

    String getTag();

    void setTag(String tag);

    String getMethod();

    void setMethod(String method);

    Class<?> getType();

    void setType(Class<?> type);
}
