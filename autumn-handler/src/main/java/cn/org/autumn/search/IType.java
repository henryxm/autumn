package cn.org.autumn.search;

import java.io.Serializable;

public interface IType extends Serializable {
    String getType();

    String getName();

    String getAlias();

    String getDescribe();

    boolean isShow();
}
