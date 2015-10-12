package bbs;

import java.util.Date;

import javax.servlet.ServletContext;

import com.blade.BladeWebContext;

import bbs.kit.BBSKit;
import blade.kit.DateKit;
import blade.kit.StringKit;

public class Funcs {

	/**
	 * 获取相对路径
	 * @param path
	 * @return
	 */
	public static String base_url(String path) {
		ServletContext servletContext = BladeWebContext.servletContext();
		String ctx = servletContext.getContextPath();
		if(StringKit.isBlank(path)){
			return ctx;
		}
		
		String val = ctx + "/" + path;
		
		return val.replaceAll("//", "/");
	}
	
	/**
	 * 格式化日期
	 * @param date
	 * @return
	 */
	public static String fmtdate(Date date) {
		if(null != date){
			return DateKit.dateFormat(date);
		}
		return "";
	}
	
	/**
	 * 格式化日期
	 * @param date
	 * @param patten
	 * @return
	 */
	public static String fmtdate(Date date, String patten) {
		if(null != date){
			return DateKit.dateFormat(date, patten);
		}
		return "";
	}
	
	public static String today(String patten){
		return fmtdate(new Date(), patten);
	}
	/**
	 * 截取字符串个数
	 * @param str
	 * @param count
	 * @return
	 */
	public static String str_count(String str, int count){
		if(StringKit.isNotBlank(str) && count > 0){
			if(str.length() <= count){
				return str;
			}
			return str.substring(0, count);
		}
		return "";
	}
	
	/**
	  * 显示时间，如果与当前时间差别小于一天，则自动用**秒(分，小时)前，如果大于一天则用format规定的格式显示
	  * 
	  * @param ctime时间
	  * @return
	  */
	public static String timespan(Date ctime) {
		String r = "";
		if (ctime == null)
			return r;

		long nowtimelong = System.currentTimeMillis();
		long ctimelong = ctime.getTime();
		long result = Math.abs(nowtimelong - ctimelong);
		
		// 20秒内
		if (result < 20000){
			r = "刚刚";
		} else if (result >= 20000 && result < 60000) {
			// 一分钟内
			long seconds = result / 1000;
			r = seconds + "秒钟前";
		} else if (result >= 60000 && result < 3600000) {
			// 一小时内
			long seconds = result / 60000;
			r = seconds + "分钟前";
		} else if (result >= 3600000 && result < 86400000) {
			// 一天内
			long seconds = result / 3600000;
			r = seconds + "小时前";
		} else {
			long days = result / 3600000 / 24;
			r = days + "天前";
		}
		return r;
	}
	
	/*
	 * markdown转html
	 */
	public static String mdTohtml(String md){
		if(StringKit.isNotBlank(md)){
			String member = base_url("/member/");
			//@功能的发送回复提醒
			md = md.replaceAll("@([a-z0-9]{3,12}\\s)", "@<a href='"+ member +"$1' target='_blank'>$1</a>&nbsp;");
			return BBSKit.mdToHtml(md);
		}
		return "";
	}
}
