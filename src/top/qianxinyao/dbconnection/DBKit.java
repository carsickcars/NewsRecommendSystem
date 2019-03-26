package top.qianxinyao.dbconnection;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.jfinal.plugin.activerecord.ActiveRecordPlugin;
import com.jfinal.plugin.c3p0.C3p0Plugin;
import org.apache.log4j.Logger;
import org.apache.mahout.cf.taste.impl.model.jdbc.MySQLBooleanPrefJDBCDataModel;
import top.qianxinyao.model.*;

import javax.sql.DataSource;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Properties;

public class DBKit{
	
	public static final Logger logger=Logger.getLogger(DBKit.class);
	
	//偏好表表名
	public static final String PREF_TABLE="newslogs";  
	//用户id列名
	public static final String PREF_TABLE_USERID="user_id";
	//新闻id列名
	public static final String PREF_TABLE_NEWSID="news_id";
	//偏好值列名
	public static final String PREF_TABLE_PREFVALUE="prefer_degree";
	//用户浏览时间列名
	public static final String PREF_TABLE_TIME="view_time";
	
	private static C3p0Plugin cp;
	
	public static void initalize()
	{
		try
		{
			ssh(); //连接数据库之前先连接一下ssh。
			HashMap<String, String> info = getDBInfo();
			cp = new C3p0Plugin(info.get("url"), info.get("user"), info.get("password"));
			
			ActiveRecordPlugin arp = new ActiveRecordPlugin(cp);
			arp.addMapping("users", Users.class);
			arp.addMapping("news", News.class);
			arp.addMapping("newsmodules", Newsmodules.class);
			arp.addMapping("newslogs", Newslogs.class);
			arp.addMapping("recommendations", Recommendations.class);
			
			
			if(cp.start() && arp.start())
				logger.info("数据库连接池插件启动成功......");
			else
				logger.info("c3p0插件启动失败！");
			
		
			
			logger.info("数据库初始化工作完毕！");
		}
		catch (Exception e)
		{
			logger.error("数据库连接初始化错误！");
		}
		return;
	}
	
	public static HashMap<String, String> getDBInfo()
	{
		HashMap<String, String> info = null;
		try
		{
			Properties p = new Properties();
			p.load(new FileInputStream(System.getProperty("user.dir") + "/res/dbconfig.properties"));
			info = new HashMap<String, String>();
			info.put("url", p.getProperty("url"));
			info.put("user", p.getProperty("user"));
			info.put("password", p.getProperty("password"));

			info.put("sshhost",p.getProperty("sshhost"));
			info.put("sshuser",p.getProperty("sshuser"));
			info.put("sshpwd",p.getProperty("sshpwd"));
			info.put("sshport",p.getProperty("sshport"));
		}
		catch (FileNotFoundException e)
		{
			logger.error("读取属性文件--->失败！- 原因：文件路径错误或者文件不存在");
		}
		catch (IOException e)
		{
			logger.error("装载文件--->失败!");
		}
		return info;
	}
	
	public static DataSource getDataSource() {
		if(cp==null)
			initalize();
		return cp.getDataSource();
	}
	
	public static MySQLBooleanPrefJDBCDataModel getMySQLJDBCDataModel(){
	return new MySQLBooleanPrefJDBCDataModel(DBKit.getDataSource(), PREF_TABLE, PREF_TABLE_USERID,
		PREF_TABLE_NEWSID,PREF_TABLE_TIME);
	}


	public static void ssh(){
		try {
			HashMap<String, String> info = getDBInfo();
			String sshUser=info.get("sshuser");
			String sshHost=info.get("sshhost");
			String sshPwd=info.get("sshpwd");
			Integer sshPort=Integer.valueOf(info.get("sshport"));

			JSch jsch = new JSch();
			Session session = jsch.getSession(sshUser, sshHost, sshPort);
			session.setPassword(sshPwd);
			session.setConfig("StrictHostKeyChecking", "no");
			session.connect();
			System.out.println(session.getServerVersion());//这里打印SSH服务器版本信息

			int assinged_port = session.setPortForwardingL(3307, "localhost", 3306);//端口映射 转发
			System.out.println("localhost:" + assinged_port);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
