package bbs.route;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import com.blade.Blade;
import com.blade.servlet.multipart.FileItem;
import com.blade.servlet.multipart.ServletFileUpload;

import bbs.Constant;
import bbs.kit.BBSKit;
import bbs.kit.ImageKit;
import bbs.model.User;
import blade.kit.DateKit;
import blade.kit.FileKit;
import blade.kit.IOKit;
import blade.kit.StringKit;
import blade.kit.json.JsonObject;

public class UploadRoute implements RouteBase {

	@Override
	public void run() {
		Blade blade = Blade.me();
		// 上传文件
		blade.all("/files/upload", (request, response) -> {
			
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
				
				File file = new File(blade.webRoot() + File.separator + Constant.UPLOAD_FOLDER + File.separator + saveName);
				
				IOKit.write(fileItem.getFileContent(), file);
				
				JsonObject jsonObject = new JsonObject();
				
				if(file.exists()){
					
					String filePath = Constant.UPLOAD_FOLDER + "/" + saveName;
					String url = Constant.CDN_SITE + "/" + filePath;
					
					jsonObject.add("success", 1);
					jsonObject.add("message", "上传成功");
					jsonObject.add("filename", fileItem.getFileName());
					jsonObject.add("filepath", filePath);
					jsonObject.add("url", url);
				} else {
					jsonObject.add("success", 0);
					jsonObject.add("message", "上传失败");
				}
				
				response.json(jsonObject.toString());
			}
			return null;
		});
		
		// 上传图片
		blade.all("/uploadimg", (request, response) -> {
			
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
				
				String savepath = blade.webRoot() + File.separator + saveName;
				
				String normalpath = blade.webRoot() + File.separator + normalName;
				
				File file = new File(savepath);
				
				IOKit.write(fileItem.getFileContent(), file);
				
				JsonObject jsonObject = new JsonObject();
				
				if(file.exists()){
					try {
						// 如果原图宽度<200则原图=缩略图
						if(!ImageKit.resize(file, new File(normalpath), 200, 0.9f)){
							FileKit.copy(file.getPath(), normalpath);
						}
						
						String url = Constant.CDN_SITE + "/" + normalName;
						
						jsonObject.add("id", saveName);
						jsonObject.add("normal", normalName);
						jsonObject.add("url", url);
						jsonObject.add("success", 1);
						jsonObject.add("message", "上传成功");
						
					} catch (IOException e) {
						e.printStackTrace();
					}
					
				} else {
					jsonObject.add("success", 0);
					jsonObject.add("message", "上传失败");
				}
				
				response.json(jsonObject.toString());
			}
			return null;
		});
	}
	
}
