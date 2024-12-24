package cn.org.autumn.modules.lan.entity;

import com.baomidou.mybatisplus.annotations.*;
import cn.org.autumn.table.annotation.*;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@TableName("sys_language")
@Table(value = "sys_language", comment = "国家语言")
@Indexes({
        @Index(name = "nametag", indexMethod = IndexMethodEnum.BTREE, indexType = IndexTypeEnum.UNIQUE, fields = {@IndexField(field = "name"), @IndexField(field = "tag")}),
})
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

    @Column(length = 20, comment = "标签")
    @Index
    private String tag;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getZhCn() {
        return zhCn;
    }

    public void setZhCn(String zhCn) {
        this.zhCn = zhCn;
    }

    public String getEnUs() {
        return enUs;
    }

    public void setEnUs(String enUs) {
        this.enUs = enUs;
    }

    public String getZhHk() {
        return zhHk;
    }

    public void setZhHk(String zhHk) {
        this.zhHk = zhHk;
    }

    public String getKoKr() {
        return koKr;
    }

    public void setKoKr(String koKr) {
        this.koKr = koKr;
    }

    public String getJaJp() {
        return jaJp;
    }

    public void setJaJp(String jaJp) {
        this.jaJp = jaJp;
    }

    public String getTtRu() {
        return ttRu;
    }

    public void setTtRu(String ttRu) {
        this.ttRu = ttRu;
    }

    public String getFrFr() {
        return frFr;
    }

    public void setFrFr(String frFr) {
        this.frFr = frFr;
    }

    public String getDeDe() {
        return deDe;
    }

    public void setDeDe(String deDe) {
        this.deDe = deDe;
    }

    public String getViVn() {
        return viVn;
    }

    public void setViVn(String viVn) {
        this.viVn = viVn;
    }

    public String getThTh() {
        return thTh;
    }

    public void setThTh(String thTh) {
        this.thTh = thTh;
    }

    public String getMsMy() {
        return msMy;
    }

    public void setMsMy(String msMy) {
        this.msMy = msMy;
    }

    public String getIdId() {
        return idId;
    }

    public void setIdId(String idId) {
        this.idId = idId;
    }

    public String getEsEs() {
        return esEs;
    }

    public void setEsEs(String esEs) {
        this.esEs = esEs;
    }

    public String getTrTr() {
        return trTr;
    }

    public void setTrTr(String trTr) {
        this.trTr = trTr;
    }

    public String getUkUk() {
        return ukUk;
    }

    public void setUkUk(String ukUk) {
        this.ukUk = ukUk;
    }

    public String getPuPt() {
        return puPt;
    }

    public void setPuPt(String puPt) {
        this.puPt = puPt;
    }

    public String getPlPl() {
        return plPl;
    }

    public void setPlPl(String plPl) {
        this.plPl = plPl;
    }

    public String getMnMn() {
        return mnMn;
    }

    public void setMnMn(String mnMn) {
        this.mnMn = mnMn;
    }

    public String getNbNo() {
        return nbNo;
    }

    public void setNbNo(String nbNo) {
        this.nbNo = nbNo;
    }

    public String getItIt() {
        return itIt;
    }

    public void setItIt(String itIt) {
        this.itIt = itIt;
    }

    public String getHeIl() {
        return heIl;
    }

    public void setHeIl(String heIl) {
        this.heIl = heIl;
    }

    public String getElGr() {
        return elGr;
    }

    public void setElGr(String elGr) {
        this.elGr = elGr;
    }

    public String getFaIr() {
        return faIr;
    }

    public void setFaIr(String faIr) {
        this.faIr = faIr;
    }

    public String getArSa() {
        return arSa;
    }

    public void setArSa(String arSa) {
        this.arSa = arSa;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName(), getZhCn(), getEnUs(), getZhHk(), getKoKr(), getJaJp(), getTtRu(), getFrFr(), getDeDe(), getViVn(), getThTh(), getMsMy(), getIdId(), getEsEs(), getTrTr(), getUkUk(), getPuPt(), getPlPl(), getMnMn(), getNbNo(), getItIt(), getHeIl(), getElGr(), getFaIr(), getArSa());
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