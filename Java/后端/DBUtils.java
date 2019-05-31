package winning.common.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

import java.io.IOException;
import java.sql.*;
import java.util.Properties;

/**
 * 
 * Created by xwf on 2019/5/30.
 *
 */

public class DBUtils {

	
	private static final Logger LOGGER = LoggerFactory.getLogger(DBUtils.class);

	private static Properties props = null;

	static {
		try {

			Resource resource = new ClassPathResource("/application.properties");//
			props = PropertiesLoaderUtils.loadProperties(resource);
			String driveClass = props.getProperty("BlDataBase.driverClassName");
			System.out.println("============== drive class========"+ driveClass);
			Class.forName(driveClass);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			LOGGER.error(e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static Connection getConnection() {

		StringBuffer sb = new StringBuffer();
		sb.append("jdbc:");

		String driveClass = props.getProperty("BlDataBase.driverClassName");
		if(null == driveClass || "".equals(driveClass.trim())){
			LOGGER.error("no specify data source type");
			return null;
		}else if(driveClass.contains("sqlserver")){
			sb.append("sqlserver://");
		}else if(driveClass.contains("oracle")){
			sb.append(":oracle:thin:@://");
		}else if(driveClass.contains("mysql")){
			sb.append(":mysql://");
		}else{
			LOGGER.error("unsupport data source type");
			return null;
		}

		sb.append(props.getProperty("BlDataBase.host")).append(":").append(props.getProperty("BlDataBase.port"))
				.append(";databaseName=").append(props.getProperty("BlDataBase.databaseName")).append(";user=")
				.append(props.getProperty("BlDataBase.username")).append(";password=").append(props.getProperty("BlDataBase.password"));

		String dbUrl = sb.toString();

		try {
			return DriverManager.getConnection(dbUrl);
		} catch (SQLException e) {
			LOGGER.error(e.getMessage());
		}
		return null;
	}
	
	
	public static void close(Connection connection, Statement stat, ResultSet rs) {
		
		try {
			if (rs != null && !rs.isClosed()) {
				rs.close();
			}
			if (stat != null && !stat.isClosed()) {
				stat.close();
			}
			if (connection != null && !connection.isClosed()) {
				connection.close();
			}
		} catch (SQLException e) {
			LOGGER.error(e.getMessage());
		}
	}
	
	public static void close(Connection conn, Statement stat) {
		
		try {
			if (stat != null && !stat.isClosed()) {
				stat.close();
			}
			if (conn != null && !conn.isClosed()) {
				conn.close();
			}
		} catch (SQLException e) {
			LOGGER.error(e.getMessage());
		}
	}
	
}
