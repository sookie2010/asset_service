package com.web.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.servlet.ServletContext;

import org.apache.log4j.Logger;
import org.hibernate.internal.util.StringHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.alibaba.fastjson.JSON;
import com.google.zxing.WriterException;
import com.utils.QrcodeUtils;
import com.utils.enums.LzFlag;
import com.utils.enums.Role;
import com.web.dao.BgrRespository;
import com.web.dao.LzxxRespository;
import com.web.entity.Lzxx;

@Service
public class LzxxService {
	private static Logger log = Logger.getLogger(LzxxService.class);
	@Autowired
	private LzxxRespository lzxxResp;
	
	@Autowired
	private BgrRespository bgrResp;
	
	@Autowired
	private QrcodeUtils qrcodeUtils;
	
	@Value("${asset.upload.rootpath}")
	private String uploadRootpath;
	
	@Value("${asset.upload.datedir}")
	private String uploadDatedir;
	
	/**
	 * 保存流转信息
	 * @param zcIds 资产ID
	 * @param flag 标识(用于区分 出库 流转 回收)
	 * @param bgr 操作用户(可能是MA或者MK)
	 * @return 操作ID
	 */
	public String save(Set<String> zcIds, LzFlag flag, String bgrId) {
		String fcrId = null; //发出人
		String jsrId = null; //接受人
		if(bgrId != null) {
			List<String> qxList = bgrResp.queryQxByBgr(bgrId);//当前用户具备的权限
			switch(flag) {
			case CK : 
				if(qxList.contains(Role.MA.getCode())) {
					//当前用户是材料员
					fcrId = bgrId;
				} else if(qxList.contains(Role.MK.getCode())) {
					//当前用户是保管员
					jsrId = bgrId;
				} else {
					log.warn("当前用户不具备操作权限!");
					return null;
				}
				break;
			case LZ : break; //TODO
			case HS : break; //TODO
			default : 
				log.warn("未知的操作类型 : " + flag);
			}
		} else {
			log.warn("用户未登录!");
			return null;
		}
		
		List<String> lzIds = new ArrayList<String>();
		String operateId = UUID.randomUUID().toString();
		for(String zcId : zcIds) {
			Lzxx lzxx = new Lzxx();
			lzxx.setOperateID(operateId);//操作ID
			lzxx.setBiaozhi(flag.getFlag().toString());
			lzxx.setFkZichanZcID(zcId);
			lzxx.setLzsj(new Date());
			lzxx.setFkBgrFcrID(fcrId); //发出人
			lzxx.setFkBgrJsrID(jsrId); //接受人
			lzxx.setStatus(0); //状态 : 0未完成
			lzIds.add(lzxxResp.save(lzxx).getUuid());
		}
		return operateId;
	}
	/**
	 * 输出二维码图片
	 * @param operateId 操作ID
	 * @param output 字节输出流
	 */
	public void outputQrcode(String operateId, OutputStream output) {
		List<Lzxx> result = lzxxResp.findByOperateID(operateId);
		if(result == null || result.isEmpty()) { //按照操作ID未查到数据
			return;
		}
		Map<String, Object> qrcodeContent = new HashMap<String, Object>();
		qrcodeContent.put("operateType", result.get(0).getBiaozhi()); //操作类型 1.出库 2.流转 3.回收
		qrcodeContent.put("operateId", operateId);//多个流转ID
		try {
			qrcodeUtils.createQrcode(JSON.toJSONString(qrcodeContent), output);
		} catch (WriterException | IOException e) {
			log.error("生成二维码失败", e);
		}
	}
	/**
	 * 将上传的文件写入到服务器本地
	 * @param photo
	 * @param context
	 * @return 图片文件保存路径
	 */
	public String writeFile(MultipartFile photo, ServletContext context) {
		DateFormat formatter = new SimpleDateFormat(uploadDatedir);
		
		String datePath = formatter.format(new Date());
		File outputPath = new File(context.getRealPath(uploadRootpath + datePath));
		if(!outputPath.exists()) {
			outputPath.mkdirs(); //如果目录不存在则直接创建
		}
		String fileName = photo.getOriginalFilename(); //选择的文件原本的名字
		
		String fileUUID = UUID.randomUUID().toString();
		String ext = fileName.substring(fileName.lastIndexOf("."), fileName.length());
		
		log.info("fileUUID : " + fileUUID);
		byte[] buf = new byte[1024];
		try{
			try(InputStream input = photo.getInputStream(); 
					OutputStream output = new FileOutputStream(outputPath.getAbsolutePath() + "/" + fileUUID + ext)){
				int len = input.read(buf);
				while(len > 0) {
					output.write(buf, 0, len);
					len = input.read(buf);
				}
			}
			return uploadRootpath + datePath + "/" + fileUUID + ext;
		} catch (IOException e) {
			log.error("文件上传错误!", e);
			return null;
		}
	}
	
	public Lzxx findByOperateIdAndZcId(String operateId, String zcId) {
		return lzxxResp.findByOperateIDAndFkZichanZcID(operateId, zcId);
	}
	/**
	 * 更新流转信息当中的照片URL
	 * @param lzxx
	 * @param photoPath 照片保存的**相对**路径
	 */
	public void updatePhotoId(Lzxx lzxx, String photoPath, ServletContext context) {
		String oldPath = lzxx.getFkZhaopianPzzpURL();
		if(StringHelper.isNotEmpty(oldPath)) {
			//如果原文件存在, 则删除
			File oldFile = new File(context.getRealPath(oldPath));
			if(oldFile.exists()) {
				oldFile.delete();
			}
		}
		lzxx.setFkZhaopianPzzpURL(photoPath);
		lzxxResp.save(lzxx);
	}
	/**
	 * 验证该操作ID对应的流转信息是否已经全部完成
	 * @param operateId 操作ID
	 * @return 全部完成返回true, 否则返回false
	 */
	public boolean checkFinished(String operateId) {
		return lzxxResp.checkFinished(operateId) == 1;
	}
	/**
	 * 将该操作ID对应的流转数据状态改为1 (已完成)
	 * @param operateId 操作ID
	 * @return update影响的行数(大于0代表操作成功)
	 */
	public int finished(String operateId, String bgrId) {
		return lzxxResp.finished(operateId, bgrId);
	}
	/**
	 * 统计一次流转中资产的类型数量(涉及几种类型的物资)
	 * @param operateId 操作ID
	 * @return
	 */
	public int typeCount(String operateId) {
		return lzxxResp.typeCount(operateId);
	}
	
	public List<Lzxx> findByOperateID(String operateID) {
		return lzxxResp.findByOperateID(operateID);
	}
	
	public List<Object[]> getLzxxDetail(String operateId) {
		return lzxxResp.getLzxxDetail(operateId);
	}
}
