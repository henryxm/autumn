package cn.org.autumn.modules.sys.entity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class JsonConfigRefreshResult implements Serializable {
    private static final long serialVersionUID = 1L;

    private String paramKey;
    private String className;
    private boolean changed;
    private List<String> addedFields = new ArrayList<>();
    private List<String> fixes = new ArrayList<>();
    private String message;

    public void addAddedField(String field) {
        if (field != null && !field.isEmpty()) {
            addedFields.add(field);
        }
    }
}
