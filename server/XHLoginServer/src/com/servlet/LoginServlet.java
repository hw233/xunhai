package com.servlet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import com.common.Config;
import com.common.DateService;
import com.common.GCCContext;
import com.common.MD5Service;
import com.domain.Account;
import com.domain.config.BaseServerConfig;
import com.service.IAccountService;
import com.service.IBaseDataService;
import com.util.CommonUtil;
import com.util.HttpUtil;
import com.util.LogUtil;

/**
 * 账户登录
 * @author ken
 * 2014-3-8
 */
public class LoginServlet extends BaseServlet {

	private static final long serialVersionUID = 5681739853641641114L;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		doPost(req, resp);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {

		try {
			if("xh".equals(Config.AGENT)){
				//迅海
				doLogin(req, resp);
			}else if("donghai".equals(Config.AGENT)){
				//东海
				donghaiLogin(req, resp);
			}else if("yunyou".equals(Config.AGENT)){
				//云游
				yunyouLogin(req, resp);
			}else if("zhongfu".equals(Config.AGENT)){
				//中富
				zhongfuLogin(req, resp);
			}
			

		} catch (Exception e) {
			LogUtil.error("登陆异常: ",e);
			return;
		}
	}

	/**
	 * 处理账户登录 appid=xxx&userId=xxx&userName=xxx&passWord=xxx&tourist=xxx&token=xxx&time=xxx&sign=xxx
	 */
	private void doLogin(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException, Exception {

		JSONObject result = new JSONObject();
		
		String appid = req.getParameter("appid");
		String userId = req.getParameter("userId");
		String userName = req.getParameter("userName");
		String passWord = req.getParameter("passWord");
		String tourist = req.getParameter("tourist");
		String token = req.getParameter("token");
		String time = req.getParameter("time");
		String sign = req.getParameter("sign");
		
		
		if(userName == null || userName.trim().equals("")){
			//1:账号为空
			result.put("result", 1);
			this.postData(resp, result.toString());
			return;
		}
		
		if(passWord == null || passWord.trim().equals("")){
			//2:密码为空
			result.put("result", 2);
			this.postData(resp, result.toString());
			return;
		}
		
		int pwdLenth = passWord.length();
		if(pwdLenth < 6 || pwdLenth > 15){
			//3:密码长度有误
			result.put("result", 3);
			this.postData(resp, result.toString());
			return;
		}
		
		if(time == null || time.trim().equals("")  || sign == null || sign.trim().equals("")
				|| tourist == null || tourist.trim().equals("")){
			//6 登录失败
			result.put("result", 6);
			this.postData(resp, result.toString());
			return;
		}
		
		
		// 验证sign
		String realSign = MD5Service.encryptToLowerString(appid+userId+userName+passWord+tourist+token+time+Config.WEB_LOGIN_KEY);
		if (!realSign.equalsIgnoreCase(sign)){
			//6 登录失败
			result.put("result", 6);
			this.postData(resp, result.toString());
			return;
		}
		
		this.postData(resp, this.sucLogin(0, userName, passWord, null, Integer.valueOf(tourist)));
	}

	/**
	 * 东海运营  处理账户登录 https://github.com/donghaigame/DHSDKServerDemo
	 */
	private void donghaiLogin(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException, Exception {
		
		String appid = req.getParameter("appid");
		String userId = req.getParameter("userId");
		String userName = req.getParameter("userName");
		String passWord = req.getParameter("passWord");
		String tourist = req.getParameter("tourist");
		String token = req.getParameter("token");
		String time = req.getParameter("time");
		String sign = req.getParameter("sign");
		
		JSONObject result = new JSONObject();
		
		if(userId == null || "".equals(userId.trim()) 
		   || userName == null || "".equals(userName.trim())
		   || time == null || "".equals(time.trim())
		   || sign == null || "".equals(sign.trim())){
			//登录失败
			LogUtil.error("donghaiLogin 参数有误");
			result.put("result", 6);
			this.postData(resp, result.toString());
			return;
		}
		
		// 验证sign
		String realSign = MD5Service.encryptToLowerString(appid+userId+userName+passWord+tourist+token+time+Config.WEB_LOGIN_KEY);
		if (!realSign.equalsIgnoreCase(sign)){
			//6 登录失败
			LogUtil.error("donghaiLogin sign验证有误");
			
			result.put("result", 6);
			this.postData(resp, result.toString());
			return;
		}
		
		this.postData(resp, this.sucLogin(Long.valueOf(userId), userName, "123456", null, 1));
		
	}
	
	/**
	 * 云游运营  处理账户登录  http://lll.lygames.cc/index.php?m=index&c=user&a=Token&uid=客户端传给的uid&token=token 码
	 */
	private void yunyouLogin(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException, Exception {
		
		String userName = req.getParameter("userName");
		String token = req.getParameter("token");
		
		JSONObject result = new JSONObject();
		
		if(userName == null || "".equals(userName.trim()) 
		   || token == null || "".equals(token.trim())){
			//登录失败
			result.put("result", 6);
			this.postData(resp, result.toString());
			return;
		}
		
		String url = "http://lll.lygames.cc/index.php";
		
		StringBuilder param = new StringBuilder();
		param.append("?");
		param.append("m=index");
		param.append("&c=user");
		param.append("&a=Token");
		param.append("&uid=").append(userName);
		param.append("&token=").append(token);
		
		String js = HttpUtil.sendGet(url, param.toString());
		if(js == null){
			//登录失败
			result.put("result", 6);
			this.postData(resp, result.toString());
			return;
		}
		
		JSONObject resultJson = new JSONObject(js);
		if(resultJson.getString("isSuccess") .equals("1")){

			this.postData(resp, this.sucLogin(0, userName, "123456", null, 1));
		}else{
			//登录失败
			result.put("result", 6);
			this.postData(resp, result.toString());
		}
	}
	
	/**
	 * 中富登录
	 */
	private void zhongfuLogin(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException, Exception {
		JSONObject result = new JSONObject();
		
		String appid = req.getParameter("appid");
		if(appid == null){
			//登录失败
			LogUtil.error("zhongfuLogin appid："+appid);
			result.put("result", 6);
			this.postData(resp, result.toString());
			return;
		}
		
		if(appid.equals("1271")){
			//仙剑长安
			String sessionId = req.getParameter("token");
			if(sessionId == null){
				//登录失败
				LogUtil.error("zhongfuLogin SFToken："+sessionId);
				result.put("result", 6);
				this.postData(resp, result.toString());
				return;
			}
			
			String Login_Key = "02906a907397b816cfbf4fa5b4e6a6f7";
			String url = "https://api.nkyouxi.com/sdkapi.php";
			
            int time = ParseNow();
            String param = String.format("ac=check&appid=%s&sdkversion=2.1.7&sessionid=%s&time=%s", appid, GetEncode(sessionId), time);
            String code = String.format("ac=check&appid=%s&sdkversion=2.1.7&sessionid=%s&time=%s%s", appid, GetEncode(sessionId.replace(" ", "+")), time, Login_Key);
            String sign = getMD5(code);
            param += "&sign=" + sign;
            
            String js = HttpUtil.httpsPost(url, param, "application/x-www-form-urlencoded", null);
            //如果成功的话返回值格式：{"userInfo":{"username":"yx598640","uid":"6596"},"code":1}
            if (js == null){
				//登录失败
				result.put("result", 6);
				this.postData(resp, result.toString());
				return;
            }
        	JSONObject resultJson = new JSONObject(js);
        	int jsCode = resultJson.getInt("code");
        	if(jsCode == 1){
        		JSONObject userInfo = resultJson.getJSONObject("userInfo");
        		long userId = userInfo.getLong("uid");
        		String userName = userInfo.getString("username");
        		
        		this.postData(resp, this.sucLogin(userId, userName, "123456", null, 1));
        	}else{
				//登录失败
        		LogUtil.error("zhongfuLogin login返回错误："+js);
				result.put("result", 6);
				this.postData(resp, result.toString());
        	}
		}else if(appid.equals("324")){
			//百转修仙
			String uid = req.getParameter("userId");
			String userName = req.getParameter("userName");
			String token = req.getParameter("token");
			
			if(uid == null || userName == null || token == null){
				//登录失败
				LogUtil.error("百转修仙登录失败  uid："+uid);
				result.put("result", 6);
				this.postData(resp, result.toString());
				return;
			}
			
			String Login_Key = "d5bc0399e17f4dca8c1e5af4d138dfe2";
			String url = "http://cfsdk2.cftdcm.com:8080/payserver/account/check_login";
			
			StringBuilder param = new StringBuilder();
			param.append("app="+appid);
			param.append("&token="+token);
			param.append("&ts="+System.currentTimeMillis());
			param.append("&uin=").append(uid);
			String sign = MD5Service.encryptToLowerString(param.toString() + Login_Key);
			param.append("&sign=").append(sign);
			
			String js = HttpUtil.httpsRequest(url, "POST", "?"+param.toString());
			if(js.equals("0")){
				
				this.postData(resp, this.sucLogin(Long.valueOf(uid), userName, "123456", null, 1));
			}else {
				//登录失败
				LogUtil.error("百转修仙登录失败  js："+js);
				result.put("result", 6);
				this.postData(resp, result.toString());
			}
			
		}else if(appid.equals("799")){
			//大唐山海缘
			String uid = req.getParameter("userId");
			String token = req.getParameter("token");
			
			if(uid == null  || token == null){
				//登录失败
				LogUtil.error("百转修仙登录失败  uid："+uid);
				result.put("result", 6);
				this.postData(resp, result.toString());
				return;
			}
			
			String url = "http://apiqw.3z.cc/newapi.php/User/check_login_token";
			
			StringBuilder param = new StringBuilder();
			param.append("?uid="+uid);
			param.append("&token="+token);
			String js = HttpUtil.httpsRequest(url, "POST", param.toString());
			JSONObject resultJson = new JSONObject(js);
			if(resultJson.getString("status") .equals("1")){

				this.postData(resp, this.sucLogin(Long.valueOf(uid), uid, "123456", null, 1));
			}else{
				//登录失败
				result.put("result", 6);
				this.postData(resp, result.toString());
			}
		}
		
	}
	
	/**
	 * 成功登录后
	 * @throws Exception 
	 */
	private String sucLogin(long userId, String userName, String passWord, String telephone, int tourist) throws Exception{
		IAccountService accountService = GCCContext.getInstance().getServiceCollection().getAccountService();
		IBaseDataService baseDataService = GCCContext.getInstance().getServiceCollection().getBaseDataService();
		
		JSONObject result = new JSONObject();
		
		Account account = accountService.getAccountByUserName(userName.trim());
		if(account == null){
			if(tourist == 0){
				//4:账号未注册
				result.put("result", 4);
				return result.toString();
			}else{
				account = accountService.createAccount(0, userName, passWord, null, 1);
			}
		}else{
			if(!account.getPassWord().trim().equals(passWord)){
				//5密码错误
				result.put("result", 5);
				return result.toString();
			}
		}
		
		//玩家已创角的服务器列表
		List<Integer> svrList = account.getServerList();
		
		//所有服务器列表
		List<BaseServerConfig> serverList = baseDataService.listServers();
		
		long time = System.currentTimeMillis();
		String key = CommonUtil.randomLoginKey(account.getUserName());
		
		result.put("result", 0);
		result.put("userId", account.getUserId());
		result.put("key", key);
		result.put("time", time);
		if(account.getTelephone() == null){
			result.put("telePhone", 0);
		}else{
			result.put("telePhone", account.getTelephone());
		}
		
		result.put("sign", MD5Service.encryptToUpperString(account.getUserId()+key+time+Config.WEB_LOGIN_KEY));
		
		long curTime = System.currentTimeMillis();
		
		List<JSONObject> jsonList = new ArrayList<JSONObject>();
		for(BaseServerConfig model : serverList){
			Integer serverNo = model.getServerNo();
			JSONObject json = new JSONObject();
			json.put("serverNo", model.getServerNo());
			json.put("serverName", model.getServerName());
			json.put("gameHost", model.getGameHost());
			json.put("gamePort", model.getGamePort());
			json.put("loginFlag", svrList.contains(serverNo)? 1 : 0);
			json.put("endStopTime", 0);
			json.put("severType", model.getSeverType());
			long openServerDate = DateService.getDateByString(model.getOpenServerDate()).getTime();
			json.put("openServerDate", openServerDate);
			int serverState = model.getSeverState();
			if(model.getEndStopDate() != null){
				long endStopTime = DateService.getDateByString(model.getEndStopDate()).getTime();
				if(curTime < endStopTime){
					serverState = 4;
					json.put("endStopTime", endStopTime);
				}
			}
			json.put("severState", serverState);
			
			jsonList.add(json);
		}
		result.put("serverList", jsonList.toString());
		
		return result.toString();
	}
}
