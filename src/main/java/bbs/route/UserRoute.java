package bbs.route;

import java.util.Date;
import java.util.List;
import java.util.Map;

import bbs.Constant;
import bbs.Funcs;
import bbs.kit.BBSKit;
import bbs.model.User;
import bbs.service.CommentService;
import bbs.service.TopicService;
import bbs.service.UserService;
import blade.Blade;
import blade.annotation.Inject;
import blade.kit.DateKit;
import blade.kit.EncrypKit;
import blade.kit.StringKit;
import blade.plugin.sql2o.Page;
import blade.plugin.sql2o.WhereParam;
import blade.render.ModelAndView;

public class UserRoute implements RouteBase {
	
	@Inject
	private UserService userService;
	
	@Inject
	private TopicService topicService;
	
	@Inject
	private CommentService commentService;
	
	@Override
	public void run() {
		
		// 用户设置
		Blade.get("/settings", (request, response) -> {
			User user = verifySignin();
			if(null == user){
				response.go("/");
				return null;
			}
			ModelAndView modelAndView = this.getFrontModelAndView("settings");
			return modelAndView;
		});
		
		// 会员详情
		Blade.get("/member/:username", (request, response) -> {
			String username = request.pathParam("username");
			if(StringKit.isBlank(username)){
				response.go("/");
				return null;
			}
			User user = userService.getByUsername(username);
			if(null == user || user.getIs_active() == 0){
				response.go("/");
				return null;
			}
			
			Map<String, Object> profile = userService.getProfile(user.getUid());
			ModelAndView modelAndView = this.getFrontModelAndView("member_detail");
			modelAndView.add("profile", profile);
			
			WhereParam where = WhereParam.me().eq("a.uid", user.getUid()).eq("a.status", 1);
			
			// 该用户最近主题
			List<Map<String, Object>> recent_topics = topicService.getTopicRecent(where, 1, 15, null).getResults();
			modelAndView.add("recent_topics", recent_topics);
			
			// 最新回复
			WhereParam whereParam = WhereParam.me();
			whereParam.eq("a.uid", user.getUid());
			
			Page<Map<String, Object>> commentPage = commentService.getComments(whereParam, 1, 15);
			modelAndView.add("commentPage", commentPage);
			
			Date time = DateKit.getDateByUnixTime(DateKit.getCurrentUnixTime() - 86400*90);
			WhereParam hotwhere = WhereParam.me().eq("a.status", 1).greaterThan("a.addtime", time);
			List<Map<String, Object>> hot_topics = topicService.getTopicRecent(hotwhere, 1, 10, "view desc").getResults();
			modelAndView.add("hot_topics", hot_topics);
			
			// 收藏状态
			Integer uid = null;
			User loginuser = request.session().attribute(Constant.LOGIN_SESSION);
			if(null != loginuser){
				uid = loginuser.getUid();
			}
			
			boolean isfollow = userService.isFollow(uid, user.getUid(), "user");
			modelAndView.add("isfollow", isfollow);
					
			return modelAndView;
		});
		
		// 保存用户信息
		Blade.post("/user/edit", (request, response) -> {
			User user = verifySignin();
			if(null == user){
				response.go("/");
				return null;
			}
			
			String type = request.query("type");
			if(StringKit.isBlank(type)){
				response.go("/");
				return null;
			}
			
			ModelAndView modelAndView = this.getFrontModelAndView("settings");
			
			// 基本信息
			if(type.equals("base")){
				String email = request.query("email");
				String qq = request.query("qq");
				String location = request.query("location");
				String homepage = request.query("homepage");
				String signature = request.query("signature");
				String introduction = request.query("introduction");
				
				if(StringKit.isBlank(email)){
					modelAndView.add("base_error", "邮箱不能为空！");
					return modelAndView;
				}
				
				if(!BBSKit.isEmail(email)){
					modelAndView.add("base_error", "邮箱格式错误！");
					return modelAndView;
				}
				
				boolean isModifyEmail = false;
					
				if(!email.equals(user.getEmail())){
					User emailuser = userService.getByEmail(email);
					if(null != emailuser){
						modelAndView.add("base_error", "该邮箱已经被占用！");
						return modelAndView;
					} else {
						// 修改了邮箱
						isModifyEmail = true;
					}
				}
				
				user = userService.updateInfo(user.getUid(), email, qq, location, homepage, signature, introduction);
				
				// 刷新登录用户session
				request.session().attribute(Constant.LOGIN_SESSION, user);
				if(isModifyEmail){
					modelAndView.add("base_success", "邮箱修改成功，请验证邮箱并修改密码！");
					return modelAndView;
				}
				
				response.go("/settings");
				
				return null;
			}
			
			// 更新头像
			if(type.equals("avatar")){
				
				String avatar = request.query("avatar");
				if(StringKit.isNotBlank(avatar)){
					// 更新用户头像
					user = userService.updateAvatar(user.getUid(), avatar);
					// 刷新登录用户session
					request.session().attribute(Constant.LOGIN_SESSION, user);
					response.go("/settings");
					return null;
				}
			}
			
			// 修改密码
			if(type.equals("pwd")){
				String curpwd = request.query("curpwd");
				String newpwd = request.query("newpwd");
				String renewpwd = request.query("renewpwd");
				if(StringKit.isBlank(curpwd)){
					modelAndView.add("pwd_error", "请输入当前密码！");
					return modelAndView;
				}
				if(StringKit.isBlank(newpwd)){
					modelAndView.add("pwd_error", "请输入新密码！");
					return modelAndView;
				}
				if(StringKit.isBlank(renewpwd)){
					modelAndView.add("pwd_error", "请确认新密码！");
					return modelAndView;
				}
				if(!renewpwd.equals(newpwd)){
					modelAndView.add("pwd_error", "新密码输入不一致！");
					return modelAndView;
				}
				
				if(!EncrypKit.md5(user.getUsername() + curpwd).equalsIgnoreCase(user.getPassword())){
					modelAndView.add("pwd_error", "当前密码错误！");
					return modelAndView;
				}
				
				modelAndView.add("pwd_success", "密码修改成功，请重新登录！");
			}
			return modelAndView;
		});
		
		// 我关注的人
		Blade.get("/follow/user", (request, response) -> {
			User user = verifySignin();
			if(null == user){
				String path = Funcs.base_url("/signin");
				response.html("<script>alert('请登录后进行操作！');location.href='"+ path +"';</script>");
				return null;
			}
			
			ModelAndView modelAndView = this.getFrontModelAndView("home");
			// 我关注的所有人
			List<Map<String, Object>> following = userService.followUsers(user.getUid());
			
			Date time = DateKit.getDateByUnixTime(DateKit.getCurrentUnixTime() - 86400*90);
			WhereParam where = WhereParam.me().eq("a.status", 1).greaterThan("a.addtime", time);
			
			Page<Map<String, Object>> topicPage = userService.followTopics(where, 1, 15);
			modelAndView.add("site_title", "我关注的人");
			modelAndView.add("welcome_msg", "我关注的人的最新主题");
			modelAndView.add("following", following);
			modelAndView.add("topicPage", topicPage);
			return modelAndView;
		});
		
		// 我关注的节点
		Blade.get("/follow/nodes", (request, response) -> {
			User user = verifySignin();
			if(null == user){
				String path = Funcs.base_url("/signin");
				response.html("<script>alert('请登录后进行操作！');location.href='"+ path +"';</script>");
				return null;
			}
			
			ModelAndView modelAndView = this.getFrontModelAndView("home");
			// 我关注的所有节点
			List<Map<String, Object>> nodes = userService.followNodes(user.getUid());
			modelAndView.add("site_title", "我关注的节点");
			modelAndView.add("welcome_msg", "我关注的人的节点列表");
			modelAndView.add("nodes", nodes);
			return modelAndView;
		});
		
		Blade.get("/user/follow/:uid", (request, response) -> {
			User user = verifySignin();
			if(null == user){
				String path = Funcs.base_url("/signin");
				response.html("<script>alert('请登录后进行操作！');location.href='"+ path +"';</script>");
				return null;
			}
			Integer uid = request.pathParamToInt("uid");
			if(null != uid){
				userService.follow(user.getUid(), uid, "user");
			}
			// 更新用户关注人数
			userService.updateCount(user.getUid(), "user", true);
			request.session().attribute(Constant.LOGIN_SESSION, userService.getByUID(user.getUid()));
			response.go("/member/" + userService.getByUID(uid).getUsername());
			return null;
		});
		
		Blade.get("/user/unfollow/:uid", (request, response) -> {
			User user = verifySignin();
			if(null == user){
				response.go("/");
				return null;
			}
			Integer uid = request.pathParamToInt("uid");
			if(null != uid){
				userService.unfollow(user.getUid(), uid, "user");
			}
			// 更新用户关注人数
			userService.updateCount(user.getUid(), "user", false);
			request.session().attribute(Constant.LOGIN_SESSION, userService.getByUID(user.getUid()));
			response.go("/member/" + userService.getByUID(uid).getUsername());
			return null;
		});
		
		/**
		 * 签到
		 */
		Blade.post("/clock-in", (req, res) -> {
			User user = verifySignin();
			if(null == user){
				res.go("/");
				return null;
			}
			return null;
		});
		
	}
	
}
