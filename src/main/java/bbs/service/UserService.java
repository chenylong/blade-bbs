package bbs.service;

import java.util.List;
import java.util.Map;

import bbs.model.User;
import blade.plugin.sql2o.Page;
import blade.plugin.sql2o.WhereParam;

public interface UserService {

	public User getByUID(Integer uid);
	
	public User getByUsername(String username);
	
	public User getByEmail(String email);
	
	public Map<String, Object> getProfile(Integer uid);
	
	public User signup(String username, String password, String email);
	
	public User signin(String username, String password);
	
	public boolean updateCount(Integer uid, String type, boolean add);

	public void noticeTo0(Integer uid);
	
	public boolean isFollow(Integer uid, Integer relate_id, String type);
	
	public boolean follow(Integer uid, Integer relate_id, String type);
	
	public boolean unfollow(Integer uid, Integer relate_id, String type);

	public User updateInfo(Integer uid, String email, String qq,
			String location, String homepage, String signature, String introduction);

	public User updateAvatar(Integer uid, String avatar);

	public Page<Map<String, Object>> getUsers(WhereParam whereParam, Integer page, Integer pageSize);

	public void updateUserName(Integer uid, String username, String email);
	
	public List<Map<String, Object>> followUsers(Integer uid);
	
	public Page<Map<String, Object>> followTopics(WhereParam whereParam, Integer page, Integer pageSize);

	public List<Map<String, Object>> followNodes(Integer uid);
	
}
