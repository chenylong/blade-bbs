package bbs.route;

import java.util.List;
import java.util.Map;

import com.blade.Blade;
import com.blade.annotation.Inject;
import com.blade.render.ModelAndView;

import bbs.Constant;
import bbs.model.Node;
import bbs.model.User;
import bbs.service.NodeService;
import bbs.service.TopicService;
import bbs.service.UserService;
import blade.kit.StringKit;
import blade.plugin.sql2o.Page;
import blade.plugin.sql2o.WhereParam;


public class NodeRoute implements RouteBase {

	@Inject
	private NodeService nodeService;
	
	@Inject
	private TopicService topicService;
	
	@Inject
	private UserService userService;
	
	@Override
	public void run() {
		Blade blade = Blade.me();
		
		blade.get("/node", (request, response) -> {
			ModelAndView modelAndView = this.getFrontModelAndView("node");
			List<Node> nodes = nodeService.getNodes(null, null);		
			modelAndView.add("nodes", nodes);
			return modelAndView;
		});
		
		blade.get("/go/:nkey", (request, response) -> {
			ModelAndView modelAndView = this.getFrontModelAndView("node_topic");
			String nkey = request.pathParam("nkey");
			if(StringKit.isBlank(nkey)){
				response.go("/");
				return null;
			}
			
			Node node = nodeService.getNodeByNKey(nkey);
			if(null == node){
				response.go("/");
				return null;
			}
			
			Integer page = (null == request.queryToInt("page")) ? 1 : request.queryToInt("page");
			
			WhereParam where = WhereParam.me().eq("a.nid", node.getNid()).eq("a.status", 1);
			
			Page<Map<String, Object>> topicPage = topicService.getTopicRecent(where, page, 15, null);
			modelAndView.add("topicPage", topicPage);
			modelAndView.add("node", node);
			
			// 主题数
			Integer topicCount = topicService.getCount(node.getNid());
			modelAndView.add("topicCount", topicCount);
			// 计算收藏状态
			Integer uid = null;
			User loginuser = request.session().attribute(Constant.LOGIN_SESSION);
			if(null != loginuser){
				uid = loginuser.getUid();
			}
			boolean isfollow = userService.isFollow(uid, node.getNid(), "node");
			modelAndView.add("isfollow", isfollow);
			
			return modelAndView;
		});
		
		// 收藏节点
		blade.get("/node/follow/:nkey", (request, response)->{
			User user = verifySignin();
			if(null == user){
				response.go("/");
				return null;
			}
			String nkey = request.pathParam("nkey");
			if(StringKit.isBlank(nkey)){
				return null;
			}
			Node node = nodeService.getNodeByNKey(nkey);
			if(null != node){
				userService.follow(user.getUid(), node.getNid(), "node");
			}
			response.go("/go/" + nkey);
			return null;
		});
		
		// 取消收藏节点
		blade.get("/node/follow/:nkey", (request, response)->{
			User user = verifySignin();
			if(null == user){
				response.go("/");
				return null;
			}
			String nkey = request.pathParam("nkey");
			if(StringKit.isBlank(nkey)){
				return null;
			}
			Node node = nodeService.getNodeByNKey(nkey);
			if(null != node){
				userService.unfollow(user.getUid(), node.getNid(), "node");
			}
			response.go("/go/" + nkey);
			return null;
		});
		
		
	}
	
}