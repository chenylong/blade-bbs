package bbs.route;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import bbs.Constant;
import bbs.kit.BBSKit;
import bbs.kit.ImageKit;
import bbs.model.User;
import blade.Blade;
import blade.kit.DateKit;
import blade.kit.FileKit;
import blade.kit.IOKit;
import blade.kit.StringKit;
import blade.kit.json.JSONObject;
import blade.servlet.FileItem;
import blade.servlet.ServletFileUpload;

public class UploadRoute implements RouteBase {

	@Override
	public void run() {
		
		// 上传文件
		Blade.all("/files/upload", (request, response) -> {
			
			User user = verifySignin();
			if(null == user){
				response.go("/");
				return null;
			}
			
			ServletFileUpload fileUpload = ServletFileUpload.parseRequest(request.servletRequest());
			
			boolean isMultipart = fileUpload.isMultipartContent(request.servletRequest());
			
			if(isMultipart){
				
				FileItem fileItem = fileUpload.fileItem("file");
				
				String suffix = FileKit.getExtension(fileItem.getFileName());
				if(StringKit.isNotBlank(suffix)){
					suffix = "." + suffix;
				}
				
				String saveName = DateKit.dateFormat(new Date(), "yyyyMMddHHmmssSSS")  + "_" + StringKit.getRandomChar(10) + suffix;
				
				File file = new File(Blade.webRoot() + File.separator + Constant.UPLOAD_FOLDER + File.separator + saveName);
				
				IOKit.write(fileItem.getFileContent(), file);
				
				JSONObject jsonObject = new JSONObject();
				
				if(file.exists()){
					
					String prfix = request.url().replaceFirst(request.servletPath(), "/");
					String filePath = Constant.UPLOAD_FOLDER + "/" + saveName;
					String url = Constant.CDN_SITE + "/" + filePath;
					
					jsonObject.put("success", 1);
					jsonObject.put("message", "上传成功");
					jsonObject.put("filename", fileItem.getFileName());
					jsonObject.put("filepath", filePath);
					jsonObject.put("url", url);
				} else {
					jsonObject.put("success", 0);
					jsonObject.put("message", "上传失败");
				}
				
				response.json(jsonObject.toString());
			}
			return null;
		});
		
		// 上传图片
		Blade.all("/uploadimg", (request, response) -> {
			
			User user = verifySignin();
			if(null == user){
				response.go("/");
				return null;
			}
			
			ServletFileUpload fileUpload = ServletFileUpload.parseRequest(request.servletRequest());
			
			boolean isMultipart = fileUpload.isMultipartContent(request.servletRequest());
			
			if(isMultipart){
				
				FileItem fileItem = fileUpload.fileItem("image");
				
				String suffix = FileKit.getExtension(fileItem.getFileName());
				if(StringKit.isNotBlank(suffix)){
					suffix = "." + suffix;
				}
				
				// 不是图片类型
				if(!BBSKit.isImage(suffix)){
					response.go("/settings");
					return null;
				}
				
				String fileName = DateKit.dateFormat(new Date(), "yyyyMMddHHmmssSSS")  + "_" + StringKit.getRandomChar(10);
				
				// 原图
				String saveName = Constant.UPLOAD_FOLDER + "/" + fileName + suffix;
				
				// 缩略图
				String normalName = Constant.UPLOAD_FOLDER + "/" + fileName + "_normal" + suffix;
				
				String savepath = Blade.webRoot() + File.separator + saveName;
				
				String normalpath = Blade.webRoot() + File.separator + normalName;
				
				File file = new File(savepath);
				
				IOKit.write(fileItem.getFileContent(), file);
				
				JSONObject jsonObject = new JSONObject();
				
				if(file.exists()){
					try {
						// 如果原图宽度<200则原图=缩略图
						if(!ImageKit.resize(file, new File(normalpath), 200, 0.9f)){
							FileKit.copy(file.getPath(), normalpath);
						}
						
						String url = Constant.CDN_SITE + "/" + normalName;
						
						jsonObject.put("id", saveName);
						jsonObject.put("normal", normalName);
						jsonObject.put("url", url);
						jsonObject.put("success", 1);
						jsonObject.put("message", "上传成功");
						
					} catch (IOException e) {
						e.printStackTrace();
					}
					
				} else {
					jsonObject.put("success", 0);
					jsonObject.put("message", "上传失败");
				}
				
				response.json(jsonObject.toString());
			}
			return null;
		});
	}
	
}
