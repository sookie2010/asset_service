package com.utils;

import java.util.List;

public class PageUtil {
	private int pageSize = 10;//每页的记录数量(默认值为10)
	private int pageNow = 1;//当前页(默认值为1)
	private long rowCount;//总记录数
	private int pageCount;//总页数
	private int rowStart;//查询起始的行数
	private List<?> result;
	public PageUtil(){}
	/**
	 * 设置分页值,每页记录数10条,当前第1页
	 * @param rowCount 总记录数
	 */
	public PageUtil(int rowCount){
		setRowCount(rowCount);
	}
	/**
	 * 设置分页值, 每页记录数10条
	 * @param rowCount 总记录数
	 * @param pageNow 当前页数
	 */
	public PageUtil(int rowCount,int pageNow){
		this.pageNow = pageNow;
		setRowCount(rowCount);
	}
	/**
	 * 设置分页值
	 * @param rowCount 总记录数
	 * @param pageSize 每页的记录数
	 * @param pageNow 当前页数
	 */
	public PageUtil(int rowCount,int pageSize,int pageNow){
		this.pageSize = pageSize;
		this.pageNow = pageNow;
		setRowCount(rowCount);
	}
	public int getPageSize() {
		return pageSize;
	}
	public void setPageSize(int pageSize) {
		this.pageSize = pageSize;
	}
	public int getPageNow() {
		return pageNow;
	}
	public void setPageNow(int pageNow) {
		this.pageNow = pageNow;
	}
	public long getRowCount() {
		return rowCount;
	}
	public void setRowCount(long rowCount) {
		this.rowCount = rowCount;
		//根据总行数与每页的记录数 , 计算出总页数
		pageCount = (int) ((rowCount%pageSize==0) ? (rowCount/pageSize) : (rowCount/pageSize+1));
	}
	public void setRowCount(String rowCountStr) {
		this.setRowCount(Long.parseLong(rowCountStr));
	}
	
	public int getPageCount() {
		return pageCount;
	}
	//总页数必须要通过计算获得 , 不可以进行直接设置
//	public void setPageCount(int pageCount) {
//		this.pageCount = pageCount;
//	}
	public int getRowStart() {
		//根据当前页数与每页的记录数, 计算出从第几条记录开始查询(第一条记录的索引是0)
		rowStart = (pageNow - 1) * pageSize;
		return rowStart;
	}
	//查询起始的行数必须要通过计算获得 , 不可以直接设置
//	public void setRowStart(int rowStart) {
//		this.rowStart = rowStart;
//	}
	public List<?> getResult() {
		return result;
	}
	public void setResult(List<?> result) {
		this.result = result;
	}
}
