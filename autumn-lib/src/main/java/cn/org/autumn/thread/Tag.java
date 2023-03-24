package cn.org.autumn.thread;

import java.util.Date;

public interface Tag {

    String getName();

    Date getTime();

    String getTag();

    void setName(String name);

    void setTime(Date time);

    void setTag(String tag);
}
