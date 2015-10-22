package bbs;

import java.util.Map;
import java.util.Properties;

import javax.sql.DataSource;

import com.alibaba.druid.pool.DruidDataSourceFactory;
import com.blade.Blade;
import com.blade.Bootstrap;
import com.blade.annotation.Inject;
import com.blade.route.RouteHandler;
import com.blade.servlet.Request;
import com.blade.servlet.Response;

import bbs.kit.BBSKit;
import bbs.route.admin.AdminRoute;
import bbs.route.admin.NodeRoute;
import bbs.route.admin.OptionsRoute;
import bbs.route.admin.TopicRoute;
import bbs.route.admin.UserRoute;
import bbs.service.OptionService;
import bbs.service.UserService;
import blade.kit.MailKit;
import blade.kit.PropertyKit;
import blade.kit.StringKit;
import blade.kit.log.Logger;
import blade.plugin.sql2o.Sql2oPlugin;
import blade.render.JetbrickRender;
import jetbrick.template.JetEngine;
import jetbrick.template.JetGlobalContext;
import jetbrick.template.resolver.GlobalResolver;

public class App extends Bootstrap {

	static Logger logger = Logger.getLogger(App.class);
	
	@Inject
	UserService userService;
	
	@Inject
	OptionService optionService;
	
	JetEngine jetEngine;
	
	@Override
	public void init() {
		
		Blade blade = Blade.me();
		
		blade.load(bbs.route.HomeRoute.class);
		blade.load(bbs.route.InstallRoute.class);
		blade.load(bbs.route.UserRoute.class);
		blade.load(bbs.route.NodeRoute.class);
		blade.load(bbs.route.TopicRoute.class);
		blade.load(bbs.route.CommentRoute.class);
		blade.load(bbs.route.UploadRoute.class);
		
		// 加载后台路由
		blade.load(AdminRoute.class);
		blade.load(NodeRoute.class);
		blade.load(TopicRoute.class);
		blade.load(UserRoute.class);
		blade.load(OptionsRoute.class);
		
		blade.before("/*", new RouteHandler() {
			@Override
			public Object handler(Request request, Response response) {
				if(!BBSKit.isInstall() && request.uri().indexOf("/install") == -1){
					return request.invoke("/install");
				}
				request.attribute("cdn", Constant.CDN_SITE);
				request.attribute("version", "1.0");
				return request.invoke();
			}
		});
		
		blade.config("blade.properties");
		
		// 设置模板引擎
		JetbrickRender jetbrickRender = new JetbrickRender();
		jetEngine = jetbrickRender.getJetEngine();
		blade.viewEngin(jetbrickRender);
		
		// 配置邮箱管理员
		MailKit.config(MailKit.SMTP_QQ, blade.config().get("mail.user"), blade.config().get("mail.pass"));
		
	}
	
	@Override
	public void contextInitialized() {
		
		Constant.IS_INSTALL = BBSKit.isInstall();
		
		Blade blade = Blade.me();
		
		// 配置数据库
		if(Constant.IS_INSTALL){
			try {
				Properties props = PropertyKit.getProperty("ds.properties");
				DataSource dataSource = DruidDataSourceFactory.createDataSource(props);
				Sql2oPlugin sql2oPlugin = blade.plugin(Sql2oPlugin.class);
				sql2oPlugin.config(dataSource).run();
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
				
		JetGlobalContext globalContext = jetEngine.getGlobalContext();
		Constant.CONTEXT = globalContext;
		
		GlobalResolver resolver = jetEngine.getGlobalResolver();
		resolver.registerFunctions(Funcs.class);
		resolver.registerMethods(Methods.class);
		
		// 参数初始化
		Constant.WEB_SITE = blade.config().get("web_site");
		
		Constant.CDN_SITE = blade.config().get("cnd_site");
		
		if(Constant.IS_INSTALL){
			Map<String, Object> sysinfo = optionService.getOptions();
			Constant.SYSINFO = sysinfo;
			Constant.CONTEXT.set(Map.class, Constant.SYSINFO_FIELD, Constant.SYSINFO);
			
			Constant.SITE_NAME = Constant.SYSINFO.get("site_name").toString();
			
			String theme = Constant.SYSINFO.get("site_theme").toString();
			if(StringKit.isNotBlank(theme)){
				Constant.THEME_NAME = theme;
			}
			
			Constant.MAIL_ACTIVE_TITLE = Constant.SITE_NAME + "帐号激活";
		}
		
	}
	
}