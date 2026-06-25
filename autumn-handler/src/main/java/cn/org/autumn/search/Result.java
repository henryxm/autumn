package cn.org.autumn.search;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Result implements Serializable {
    private String type;

    public Result(Class<?> clazz) {
        this.type = ISearch.getType(clazz);
    }
}
