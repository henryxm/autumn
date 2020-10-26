package cn.org.autumn.modules.lan.entity;

import com.baomidou.mybatisplus.annotations.*;
import cn.org.autumn.table.annotation.*;
import cn.org.autumn.table.data.DataType;


import java.io.Serializable;
import java.util.Date;

/**
 * 国家语言
 * 
 * @author Shaohua Xu
 * @email henryxm@163.com
 * @date 2020-10
 */
@UniqueKeys({@UniqueKey(name = "name", fields = {@UniqueKeyFields(field = "name")})})
@TableName("sys_language")
@Table(value = "sys_language", comment = "国家语言")
public class LanguageEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * id
	 */
	@TableId
	@Column(isKey = true, type = "bigint", length = 20, isNull = false, isAutoIncrement = true, comment = "id")
	private Long id;
	/**
	 * 标识
	 */
	@Column(length = 200, isNull = false, comment = "标识")
	private String name;
	/**
	 * 英语(美国)
	 */
	@Column(type = "text", comment = "英语(美国)")
	private String enUs;
	/**
	 * 简体中文(中国)
	 */
	@Column(type = "text", comment = "简体中文(中国)")
	private String zhCn;
	/**
	 * 繁体中文(香港)
	 */
	@Column(type = "text", comment = "繁体中文(香港)")
	private String zhHk;
	/**
	 * 韩语(韩国)
	 */
	@Column(type = "text", comment = "韩语(韩国)")
	private String koKr;
	/**
	 * 日语(日本)
	 */
	@Column(type = "text", comment = "日语(日本)")
	private String jaJp;
	/**
	 * 俄语(俄罗斯)
	 */
	@Column(type = "text", comment = "俄语(俄罗斯)")
	private String ttRu;
	/**
	 * 法语(法国)
	 */
	@Column(type = "text", comment = "法语(法国)")
	private String frFr;
	/**
	 * 德语(德国)
	 */
	@Column(type = "text", comment = "德语(德国)")
	private String deDe;
	/**
	 * 越语(越南)
	 */
	@Column(type = "text", comment = "越语(越南)")
	private String viVn;
	/**
	 * 泰语(泰国)
	 */
	@Column(type = "text", comment = "泰语(泰国)")
	private String thTh;
	/**
	 * 马来语(马来西亚)
	 */
	@Column(type = "text", comment = "马来语(马来西亚)")
	private String msMy;
	/**
	 * 印尼语(印尼)
	 */
	@Column(type = "text", comment = "印尼语(印尼)")
	private String idId;
	/**
	 * 西班牙语(西班牙)
	 */
	@Column(type = "text", comment = "西班牙语(西班牙)")
	private String esEs;
	/**
	 * 土耳其语(土耳其)
	 */
	@Column(type = "text", comment = "土耳其语(土耳其)")
	private String trTr;
	/**
	 * 乌克兰语(乌克兰)
	 */
	@Column(type = "text", comment = "乌克兰语(乌克兰)")
	private String ukUk;
	/**
	 * 葡萄牙语(葡萄牙)
	 */
	@Column(type = "text", comment = "葡萄牙语(葡萄牙)")
	private String puPt;
	/**
	 * 波兰语(波兰)
	 */
	@Column(type = "text", comment = "波兰语(波兰)")
	private String plPl;
	/**
	 * 蒙古语(蒙古)
	 */
	@Column(type = "text", comment = "蒙古语(蒙古)")
	private String mnMn;
	/**
	 * 挪威语(挪威)
	 */
	@Column(type = "text", comment = "挪威语(挪威)")
	private String nbNo;
	/**
	 * 意大利语(意大利)
	 */
	@Column(type = "text", comment = "意大利语(意大利)")
	private String itIt;
	/**
	 * 希伯来语(以色列)
	 */
	@Column(type = "text", comment = "希伯来语(以色列)")
	private String heIl;
	/**
	 * 希腊语(希腊)
	 */
	@Column(type = "text", comment = "希腊语(希腊)")
	private String elGr;
	/**
	 * 波斯语(伊朗)
	 */
	@Column(type = "text", comment = "波斯语(伊朗)")
	private String faIr;
	/**
	 * 阿拉伯语(沙特阿拉伯)
	 */
	@Column(type = "text", comment = "阿拉伯语(沙特阿拉伯)")
	private String arSa;

	/**
	 * 设置：id
	 */
	public void setId(Long id) {
		this.id = id;
	}
	/**
	 * 获取：id
	 */
	public Long getId() {
		return id;
	}
	/**
	 * 设置：标识
	 */
	public void setName(String name) {
		this.name = name;
	}
	/**
	 * 获取：标识
	 */
	public String getName() {
		return name;
	}
	/**
	 * 设置：英语(美国)
	 */
	public void setEnUs(String enUs) {
		this.enUs = enUs;
	}
	/**
	 * 获取：英语(美国)
	 */
	public String getEnUs() {
		return enUs;
	}
	/**
	 * 设置：简体中文(中国)
	 */
	public void setZhCn(String zhCn) {
		this.zhCn = zhCn;
	}
	/**
	 * 获取：简体中文(中国)
	 */
	public String getZhCn() {
		return zhCn;
	}
	/**
	 * 设置：繁体中文(香港)
	 */
	public void setZhHk(String zhHk) {
		this.zhHk = zhHk;
	}
	/**
	 * 获取：繁体中文(香港)
	 */
	public String getZhHk() {
		return zhHk;
	}
	/**
	 * 设置：韩语(韩国)
	 */
	public void setKoKr(String koKr) {
		this.koKr = koKr;
	}
	/**
	 * 获取：韩语(韩国)
	 */
	public String getKoKr() {
		return koKr;
	}
	/**
	 * 设置：日语(日本)
	 */
	public void setJaJp(String jaJp) {
		this.jaJp = jaJp;
	}
	/**
	 * 获取：日语(日本)
	 */
	public String getJaJp() {
		return jaJp;
	}
	/**
	 * 设置：俄语(俄罗斯)
	 */
	public void setTtRu(String ttRu) {
		this.ttRu = ttRu;
	}
	/**
	 * 获取：俄语(俄罗斯)
	 */
	public String getTtRu() {
		return ttRu;
	}
	/**
	 * 设置：法语(法国)
	 */
	public void setFrFr(String frFr) {
		this.frFr = frFr;
	}
	/**
	 * 获取：法语(法国)
	 */
	public String getFrFr() {
		return frFr;
	}
	/**
	 * 设置：德语(德国)
	 */
	public void setDeDe(String deDe) {
		this.deDe = deDe;
	}
	/**
	 * 获取：德语(德国)
	 */
	public String getDeDe() {
		return deDe;
	}
	/**
	 * 设置：越语(越南)
	 */
	public void setViVn(String viVn) {
		this.viVn = viVn;
	}
	/**
	 * 获取：越语(越南)
	 */
	public String getViVn() {
		return viVn;
	}
	/**
	 * 设置：泰语(泰国)
	 */
	public void setThTh(String thTh) {
		this.thTh = thTh;
	}
	/**
	 * 获取：泰语(泰国)
	 */
	public String getThTh() {
		return thTh;
	}
	/**
	 * 设置：马来语(马来西亚)
	 */
	public void setMsMy(String msMy) {
		this.msMy = msMy;
	}
	/**
	 * 获取：马来语(马来西亚)
	 */
	public String getMsMy() {
		return msMy;
	}
	/**
	 * 设置：印尼语(印尼)
	 */
	public void setIdId(String idId) {
		this.idId = idId;
	}
	/**
	 * 获取：印尼语(印尼)
	 */
	public String getIdId() {
		return idId;
	}
	/**
	 * 设置：西班牙语(西班牙)
	 */
	public void setEsEs(String esEs) {
		this.esEs = esEs;
	}
	/**
	 * 获取：西班牙语(西班牙)
	 */
	public String getEsEs() {
		return esEs;
	}
	/**
	 * 设置：土耳其语(土耳其)
	 */
	public void setTrTr(String trTr) {
		this.trTr = trTr;
	}
	/**
	 * 获取：土耳其语(土耳其)
	 */
	public String getTrTr() {
		return trTr;
	}
	/**
	 * 设置：乌克兰语(乌克兰)
	 */
	public void setUkUk(String ukUk) {
		this.ukUk = ukUk;
	}
	/**
	 * 获取：乌克兰语(乌克兰)
	 */
	public String getUkUk() {
		return ukUk;
	}
	/**
	 * 设置：葡萄牙语(葡萄牙)
	 */
	public void setPuPt(String puPt) {
		this.puPt = puPt;
	}
	/**
	 * 获取：葡萄牙语(葡萄牙)
	 */
	public String getPuPt() {
		return puPt;
	}
	/**
	 * 设置：波兰语(波兰)
	 */
	public void setPlPl(String plPl) {
		this.plPl = plPl;
	}
	/**
	 * 获取：波兰语(波兰)
	 */
	public String getPlPl() {
		return plPl;
	}
	/**
	 * 设置：蒙古语(蒙古)
	 */
	public void setMnMn(String mnMn) {
		this.mnMn = mnMn;
	}
	/**
	 * 获取：蒙古语(蒙古)
	 */
	public String getMnMn() {
		return mnMn;
	}
	/**
	 * 设置：挪威语(挪威)
	 */
	public void setNbNo(String nbNo) {
		this.nbNo = nbNo;
	}
	/**
	 * 获取：挪威语(挪威)
	 */
	public String getNbNo() {
		return nbNo;
	}
	/**
	 * 设置：意大利语(意大利)
	 */
	public void setItIt(String itIt) {
		this.itIt = itIt;
	}
	/**
	 * 获取：意大利语(意大利)
	 */
	public String getItIt() {
		return itIt;
	}
	/**
	 * 设置：希伯来语(以色列)
	 */
	public void setHeIl(String heIl) {
		this.heIl = heIl;
	}
	/**
	 * 获取：希伯来语(以色列)
	 */
	public String getHeIl() {
		return heIl;
	}
	/**
	 * 设置：希腊语(希腊)
	 */
	public void setElGr(String elGr) {
		this.elGr = elGr;
	}
	/**
	 * 获取：希腊语(希腊)
	 */
	public String getElGr() {
		return elGr;
	}
	/**
	 * 设置：波斯语(伊朗)
	 */
	public void setFaIr(String faIr) {
		this.faIr = faIr;
	}
	/**
	 * 获取：波斯语(伊朗)
	 */
	public String getFaIr() {
		return faIr;
	}
	/**
	 * 设置：阿拉伯语(沙特阿拉伯)
	 */
	public void setArSa(String arSa) {
		this.arSa = arSa;
	}
	/**
	 * 获取：阿拉伯语(沙特阿拉伯)
	 */
	public String getArSa() {
		return arSa;
	}
}
