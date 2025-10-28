package cn.org.autumn.search;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Result implements Serializable {
    String name;
    String type;

    public Result(Class<?> clazz) {
        this.type = clazz.getSimpleName();
        this.name = clazz.getName();
    }
}
