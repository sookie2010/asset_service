package com.web.entity;

import java.io.Serializable;
import java.math.BigDecimal;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.annotations.GenericGenerator;

/**
 * 资产表实体类
 * 
 * @author 夏夜梦星辰
 *
 */
@Entity
@Table(name = "zichan")
public class Zichan implements Serializable {
	private static final long serialVersionUID = 1L;
	
	public Zichan() {}
	public Zichan(Zichan zc) {
		//模拟实现对象深拷贝
		this.uuid = zc.getUuid();
		this.zcid = zc.getZcid();
		this.fkBgdwid = zc.getFkBgdwid();
		this.bgr = zc.getBgr();
		this.fkXmID = zc.getFkXmID();
		this.mingch = zc.getMingch();
		this.zcly = zc.getZcly();
		this.gysDcxm = zc.getGysDcxm();
		this.beizhu = zc.getBeizhu();
		this.shul = zc.getShul();
		this.ggxh = zc.getGgxh();
		this.lbie = zc.getLbie();
		this.ppcj = zc.getPpcj();
		this.danwei = zc.getDanwei();
		this.danjia = zc.getDanjia();
		this.zczt = zc.getZczt();
		this.pdzt = zc.getPdzt();
		this.status = zc.getStatus();
	}
	
	// 主键
	@Id
	@GenericGenerator(name="idGen",strategy="uuid")
	@GeneratedValue(generator="idGen")
	private String uuid ;//varchar(32) NOT NULL COMMENT 'id',
	
	@Column(name="zcid")
	private String zcid ;//varchar(32) DEFAULT NULL COMMENT '资产编码',
	
	@Column(name="fk_bgdwid")
	private String fkBgdwid ;//varchar(32) DEFAULT NULL COMMENT '保管单位编号',
	
	//@Column(name="fk_bgr_ID")
	//private String fkBgrID ;//varchar(32) NOT NULL COMMENT '保管人',
	
	@ManyToOne
	@JoinColumn(name="fk_bgr_ID")
	private Bgr bgr; //保管人
	
	@Column(name="fk_xm_ID")
	private String fkXmID ;//varchar(32) NOT NULL COMMENT '项目部',
	
	@Column(name="mingch")
	private String mingch ;//varchar(30) DEFAULT NULL COMMENT '名称',
	
	@Column(name="zcly")
	private String zcly ;//varchar(30) DEFAULT NULL COMMENT '资产来源',
	
	@Column(name="gys_dcxm")
	private String gysDcxm ;//varchar(50) DEFAULT NULL COMMENT '供应商/调出项目名称',
	
	@Column(name="beizhu")
	private String beizhu ;//varchar(100) DEFAULT NULL COMMENT '备注',
	
	@Column(name="shul")
	private BigDecimal shul ;//decimal(30,6) DEFAULT NULL COMMENT '数量',
	
	@Column(name="ggxh")
	private String ggxh ;//varchar(30) DEFAULT NULL COMMENT '规格型号',
	
	@Column(name="lbie")
	private String lbie ;//varchar(30) DEFAULT NULL COMMENT '类别',
	
	@Column(name="ppcj")
	private String ppcj ;//varchar(30) DEFAULT NULL COMMENT '品牌厂家',
	
	@Column(name="danwei")
	private String danwei ;//varchar(30) DEFAULT NULL COMMENT '单位',
	
	@Column(name="danjia")
	private BigDecimal danjia ;//decimal(30,6) DEFAULT NULL COMMENT '单价',
	
	@Column(name="zczt")
	private String zczt ;//varchar(32) DEFAULT NULL COMMENT '资产状态',
	
	@Column(name="pdzt")
	private String pdzt;//盘点状态:已盘点 未盘点
	
	@Column(name="status")
	private String status; //状态 :正常 损坏 丢失

	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	public String getZcid() {
		return zcid;
	}

	public void setZcid(String zcid) {
		this.zcid = zcid;
	}

	public String getFkBgdwid() {
		return fkBgdwid;
	}

	public void setFkBgdwid(String fkBgdwid) {
		this.fkBgdwid = fkBgdwid;
	}

	public String getFkXmID() {
		return fkXmID;
	}

	public void setFkXmID(String fkXmID) {
		this.fkXmID = fkXmID;
	}

	public String getMingch() {
		return mingch;
	}

	public void setMingch(String mingch) {
		this.mingch = mingch;
	}

	public String getZcly() {
		return zcly;
	}

	public void setZcly(String zcly) {
		this.zcly = zcly;
	}

	public String getGysDcxm() {
		return gysDcxm;
	}

	public void setGysDcxm(String gysDcxm) {
		this.gysDcxm = gysDcxm;
	}

	public String getBeizhu() {
		return beizhu;
	}

	public void setBeizhu(String beizhu) {
		this.beizhu = beizhu;
	}

	public BigDecimal getShul() {
		return shul;
	}

	public void setShul(BigDecimal shul) {
		this.shul = shul;
	}

	public String getGgxh() {
		return ggxh;
	}

	public void setGgxh(String ggxh) {
		this.ggxh = ggxh;
	}

	public String getLbie() {
		return lbie;
	}

	public void setLbie(String lbie) {
		this.lbie = lbie;
	}

	public String getPpcj() {
		return ppcj;
	}

	public void setPpcj(String ppcj) {
		this.ppcj = ppcj;
	}

	public String getDanwei() {
		return danwei;
	}

	public void setDanwei(String danwei) {
		this.danwei = danwei;
	}

	public BigDecimal getDanjia() {
		return danjia;
	}

	public void setDanjia(BigDecimal danjia) {
		this.danjia = danjia;
	}

	public String getZczt() {
		return zczt;
	}

	public void setZczt(String zczt) {
		this.zczt = zczt;
	}

	public Bgr getBgr() {
		return bgr;
	}

	public void setBgr(Bgr bgr) {
		this.bgr = bgr;
	}
	public String getPdzt() {
		return pdzt;
	}
	public void setPdzt(String pdzt) {
		this.pdzt = pdzt;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}

}
