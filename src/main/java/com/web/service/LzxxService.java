package com.web.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
import com.utils.enums.LzType;
import com.utils.enums.Role;
import com.web.dao.BgrRepository;
import com.web.dao.LzxxRepository;
import com.web.dao.ZichanRepository;
import com.web.entity.Lzxx;
import com.web.entity.Zichan;

@Service
public class LzxxService {
	private static Logger log = Logger.getLogger(LzxxService.class);
	@Autowired
	private LzxxRepository lzxxRep;
	
	@Autowired
	private ZichanRepository zichanRep;
	
	@Autowired
	private BgrRepository bgrResp;
	
	@Autowired
	private QrcodeUtils qrcodeUtils;
	
	@Value("${asset.upload.basePath}")
	private String uploadBasePath;
	
	@Value("${asset.upload.rootpath}")
	private String uploadRootpath;
	
	@Value("${asset.upload.datedir}")
	private String uploadDatedir;
	
	@Autowired
	private ZichanService zichanService;
	
	/**
	 * 保存流转信息
	 * @param zcIds 资产ID与数量
	 * @param flag 标识(用于区分 出库 流转 回收)
	 * @param bgr 操作用户(可能是MA或者MK)
	 * @return 操作ID
	 */
	public String save(Map<String, Object> zcInfoMap, LzType flag, String bgrId) {
		String fcrId = null; //发出人
		String jsrId = null; //接受人
		if(bgrId != null) {
			List<String> qxList = bgrResp.queryQxByBgr(bgrId);//当前用户具备的权限
			switch(flag) {
			case CK : //----出库----
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
			case LZ : //----流转----
				if(qxList.contains(Role.MK.getCode())) {
					//当前用户是保管员
					fcrId = bgrId;
				} else {
					log.warn("当前用户不具备操作权限!");
					return null;
				}
				break; 
			case HS : break; //TODO
			default : 
				log.warn("未知的操作类型 : " + flag);
			}
		} else {
			log.warn("用户未登录!");
			return null;
		}
		
		String operateId = UUID.randomUUID().toString();
		for(Entry<String,Object> entry : zcInfoMap.entrySet()) {
			Zichan zc = zichanRep.getOne(entry.getKey());
			BigDecimal currentNum = zc.getShul(); //现有的资产数量
			BigDecimal lzNum = null; //需要进行流转的资产数量
			if(entry.getValue() instanceof BigDecimal) {
				//entry中的value如果是整数就是Integer, 如果是小数就是BigDecimal
				lzNum = (BigDecimal)entry.getValue();
			} else if(entry.getValue() instanceof Integer) {
				lzNum = new BigDecimal((Integer)entry.getValue());
			} else {
				log.warn("未知的数据类型:"+entry.getValue().getClass().getName());
			}
			String zcId = null;
			if(lzNum.compareTo(currentNum) < 0) { 
				//需要流转的资产数量小于现有数量
				//则需要进行数据的拆分
				zc.setShul(currentNum.subtract(lzNum)); //减法
				zichanRep.save(zc);
				/*
				 * --拷贝该对象--
				 * 持久化的对象是一个代理实例
				 * 直接操作主键之后的保存更新会报错
				 */
				Zichan zcCopy = new Zichan(zc); 
				zcCopy.setUuid(null);
				zcCopy.setShul(lzNum);
				zcId = zichanRep.save(zcCopy).getUuid();
			} else {
				zcId = zc.getUuid();
			}
			Lzxx lzxx = new Lzxx();
			lzxx.setOperateID(operateId);//操作ID
			lzxx.setBiaozhi(flag.getFlag().toString());//流转类型标识(1出库 2流转 3回收)
			lzxx.setFkZichanZcID(zcId);//资产表数据的uuid
			lzxx.setLzsj(new Date());//流转时间
			lzxx.setFkBgrFcrID(fcrId); //发出人
			lzxx.setFkBgrJsrID(jsrId); //接受人
			lzxx.setStatus(0); //状态 : 0未完成
			lzxxRep.save(lzxx);
		}
		return operateId;
	}
	/**
	 * 输出二维码图片
	 * @param operateId 操作ID
	 * @param output 字节输出流
	 */
	public void outputQrcode(String operateId, OutputStream output) {
		List<Lzxx> result = lzxxRep.findByOperateID(operateId);
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
		File outputPath = new File(getAbsolutePath(uploadRootpath + datePath, context));
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
	/**
	 * 读取文件从输出流进行输出
	 * @param filePath 文件**相对**路径
	 * @param output 字节输出流
	 * @throws IOException 
	 */
	public void readFile(String filePath, OutputStream output, ServletContext context) throws IOException {
		InputStream input = new FileInputStream(getAbsolutePath(filePath, context));
		byte[] buf = new byte[1024];
		int len = input.read(buf);
		while(len > 0) {
			output.write(buf, 0, len);
			len = input.read(buf);
		}
		output.flush();
		output.close();
		input.close();
	}
	
	public Lzxx findByOperateIdAndZcId(String operateId, String zcId) {
		return lzxxRep.findByOperateIDAndFkZichanZcID(operateId, zcId);
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
			File oldFile = new File(getAbsolutePath(oldPath, context));
			if(oldFile.exists()) {
				oldFile.delete();
			}
		}
		lzxx.setFkZhaopianPzzpURL(photoPath);
		lzxxRep.save(lzxx);
	}
	/**
	 * 验证该操作ID对应的流转信息是否已经全部完成
	 * @param operateId 操作ID
	 * @return 全部完成返回true, 否则返回false
	 */
	public boolean checkFinished(String operateId) {
		return lzxxRep.checkFinished(operateId) == 1;
	}
	/**
	 * 将该操作ID对应的流转数据状态改为1 (已完成)
	 * @param operateId 操作ID
	 * @param flag 操作的标识(用于区分 出库 流转 回收)
	 * @param bgrId 执行操作的用户ID
	 * @return update影响的行数(大于0代表操作成功)
	 */
	public int finished(String operateId, LzType flag, String bgrId) {
		int result = 0;
		boolean updateFlag = true;
		if(bgrId != null) {
			List<String> qxList = bgrResp.queryQxByBgr(bgrId);//当前用户具备的权限
			switch(flag) {
			case CK : //----出库----
				if(qxList.contains(Role.MA.getCode())) {
					//当前用户是材料员
					result = lzxxRep.fcrFinished(operateId, bgrId);
				} else if(qxList.contains(Role.MK.getCode())) {
					//当前用户是保管员
					result = lzxxRep.jsrFinished(operateId, bgrId);
				} else {
					updateFlag = false;
					log.warn("当前用户不具备操作权限!");
				}
				break;
			case LZ : //----流转----
				if(qxList.contains(Role.MK.getCode())) {
					result = lzxxRep.jsrFinished(operateId, bgrId);
				} else {
					updateFlag = false;
					log.warn("当前用户不具备操作权限!");
				}
				break; 
			case HS : //TODO 回收
				updateFlag = false;
				break;
			default : 
				updateFlag = false;
				log.warn("未知的操作类型 : " + flag);
			}
			if(updateFlag) {
				zichanRep.updateBgrId(operateId);
				List<Zichan> zichans = zichanRep.findByFkBgrID(lzxxRep.getJsrIdByOperateid(operateId));
				zichanService.mergeZc(zichans);//对zcid相同的数据进行合并(数量相加)
			}
		} else {
			log.warn("用户未登录!");
		}
		return result;
	}
	/**
	 * 统计一次流转中资产的类型数量(涉及几种类型的物资)
	 * @param operateId 操作ID
	 * @return
	 */
	public int typeCount(String operateId) {
		return lzxxRep.typeCount(operateId);
	}
	
	/**
	 * 统计一次流转当中添加的照片数量
	 * @param operateId 操作ID
	 * @return 照片数量
	 */
	public int countPhotoNum(String operateId) {
		return lzxxRep.countPhotoNum(operateId);
	}
	
	public List<Lzxx> findByOperateID(String operateID) {
		return lzxxRep.findByOperateID(operateID);
	}
	
	public List<Object[]> getLzxxDetail(String operateId) {
		return lzxxRep.getLzxxDetail(operateId);
	}
	
	/**
	 * 检验一次流转操作是否已经**全部**上传照片
	 * @param operateId 操作ID
	 * @return 已全部上传返回1, 否则返回0
	 */
	public int checkUpload(String operateId) {
		return lzxxRep.checkUpload(operateId);
	}
	
	/**
	 * 根据文件的相对路径获取绝对路径(用于保存和读取文件)
	 * @param relativePath 相对路径
	 * @param context servlet上下文对象
	 * @return 绝对路径(从磁盘根路径开始)
	 */
	private String getAbsolutePath(String relativePath, ServletContext context) {
		if(StringHelper.isEmpty(uploadBasePath)) {
			return context.getRealPath(relativePath);
		} else {
			return uploadBasePath + relativePath;
		}
	}
}
