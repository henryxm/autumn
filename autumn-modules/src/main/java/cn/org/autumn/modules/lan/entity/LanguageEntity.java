package cn.org.autumn.modules.lan.entity;

import cn.org.autumn.annotation.Cache;
import com.baomidou.mybatisplus.annotations.*;
import cn.org.autumn.table.annotation.*;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Getter
@Setter
@TableName("sys_language")
@Table(value = "sys_language", comment = "国家语言")
@Indexes({
        @Index(name = "nametag", indexMethod = IndexMethodEnum.BTREE, indexType = IndexTypeEnum.UNIQUE, fields = {@IndexField(field = "name"), @IndexField(field = "tag")}),
})
@Cache({"name", "tag"})
public class LanguageEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId
    @Column(isKey = true, type = "bigint", length = 20, isNull = false, isAutoIncrement = true, comment = "id")
    private Long id;

    @Column(length = 200, comment = "标识")
    @Index
    private String name;

    @Column(type = "text", comment = "简体中文(中国)")
    private String zhCn;

    @Column(type = "text", comment = "英语(美国)")
    private String enUs;

    @Column(type = "text", comment = "繁体中文(香港)")
    private String zhHk;

    @Column(type = "text", comment = "韩语(韩国)")
    private String koKr;

    @Column(type = "text", comment = "日语(日本)")
    private String jaJp;

    @Column(type = "text", comment = "俄语(俄罗斯)")
    private String ttRu;

    @Column(type = "text", comment = "法语(法国)")
    private String frFr;

    @Column(type = "text", comment = "德语(德国)")
    private String deDe;

    @Column(type = "text", comment = "越语(越南)")
    private String viVn;

    @Column(type = "text", comment = "泰语(泰国)")
    private String thTh;

    @Column(type = "text", comment = "马来语(马来西亚)")
    private String msMy;

    @Column(type = "text", comment = "印尼语(印尼)")
    private String idId;

    @Column(type = "text", comment = "西班牙语(西班牙)")
    private String esEs;

    @Column(type = "text", comment = "土耳其语(土耳其)")
    private String trTr;

    @Column(type = "text", comment = "乌克兰语(乌克兰)")
    private String ukUk;

    @Column(type = "text", comment = "葡萄牙语(葡萄牙)")
    private String puPt;

    @Column(type = "text", comment = "波兰语(波兰)")
    private String plPl;

    @Column(type = "text", comment = "蒙古语(蒙古)")
    private String mnMn;

    @Column(type = "text", comment = "挪威语(挪威)")
    private String nbNo;

    @Column(type = "text", comment = "意大利语(意大利)")
    private String itIt;

    @Column(type = "text", comment = "希伯来语(以色列)")
    private String heIl;

    @Column(type = "text", comment = "希腊语(希腊)")
    private String elGr;

    @Column(type = "text", comment = "波斯语(伊朗)")
    private String faIr;

    @Column(type = "text", comment = "阿拉伯语(沙特阿拉伯)")
    private String arSa;

    @Column(length = 20, comment = "标签:用于区分相同Name的不同客户端", defaultValue = "")
    @Index
    private String tag = "";

    @Column(length = 10, comment = "固定:通过管理系统修改后，不允许程序动态更新", defaultValue = "")
    @Index
    private String fix = "";

    @TableField(exist = false)
    private boolean update;

    @Override
    public int hashCode() {
        return Objects.hash(getName(), getZhCn(), getEnUs(), getZhHk(), getKoKr(), getJaJp(), getTtRu(), getFrFr(), getDeDe(), getViVn(), getThTh(), getMsMy(), getIdId(), getEsEs(), getTrTr(), getUkUk(), getPuPt(), getPlPl(), getMnMn(), getNbNo(), getItIt(), getHeIl(), getElGr(), getFaIr(), getArSa(), getTag());
    }

    // 合并值
    public LanguageEntity merge(LanguageEntity merge) {
        if (null == merge)
            return this;
        Field[] fields = getClass().getDeclaredFields();
        for (Field field : fields) {
            if ("id".equalsIgnoreCase(field.getName()) || "serialVersionUID".equalsIgnoreCase(field.getName()) || "name".equalsIgnoreCase(field.getName()))
                continue;
            try {
                Object t = field.get(this);
                Object f = field.get(merge);
                if (null != f && !f.equals(t)) {
                    field.setAccessible(true);
                    field.set(this, f);
                }
            } catch (Exception ignored) {
            }
        }
        return this;
    }

    public static List<LanguageMetadata> getLanguageMetadata() {
        List<LanguageMetadata> languageMetadataList = new ArrayList<>();
        Field[] fields = LanguageEntity.class.getDeclaredFields();
        for (Field field : fields) {
            if ("id".equalsIgnoreCase(field.getName()) || "serialVersionUID".equalsIgnoreCase(field.getName()) || "name".equalsIgnoreCase(field.getName()))
                continue;
            try {
                String name = field.getName();
                LanguageMetadata languageMetadata = new LanguageMetadata();
                languageMetadata.setName(name);
                languageMetadata.setEnable("zhCn".equals(name) || "enUs".equals(name));
                StringBuilder stringBuilder = new StringBuilder(name);
                char character = stringBuilder.charAt(3);
                String last = Character.toString(character).toUpperCase();
                stringBuilder.replace(3, 4, last);
                stringBuilder.insert(2, "_");
                languageMetadata.setValue(stringBuilder.toString());
                Column column = field.getAnnotation(Column.class);
                if (null != column) {
                    languageMetadata.setLabel(column.comment());
                }
                languageMetadataList.add(languageMetadata);
            } catch (Exception ignored) {
            }
        }
        return languageMetadataList;
    }
}