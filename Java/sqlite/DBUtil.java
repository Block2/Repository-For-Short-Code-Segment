package com.feeyo.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author xuwenfeng@variflight.com
 *
 */

public class DBUtil {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(DBUtil.class);
	
	static {
		try {
			Class.forName("org.sqlite.JDBC");
		} catch (ClassNotFoundException e) {
			LOGGER.error(e.getMessage());
		}
	}
	
	public static Connection getConnection() {
		
		String dbUrl = Globals.getXMLProperty("database.url");
		
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
