package com.feeyo.util;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.beanutils.BeanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DaoUtil {

	private final static Logger LOGGER = LoggerFactory.getLogger(DaoUtil.class);

	//将查询结果封装成bean
	private static <T> T autoBean(Class<T> clazz, ResultSet rs)
			throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, InstantiationException {

		T bean = clazz.newInstance();
		try {
			ResultSetMetaData rsmd = rs.getMetaData();
			int columCount = rsmd.getColumnCount();
			for (int i = 1; i <= columCount; i++) {
				String columName = rsmd.getColumnName(i);
				Object value = rs.getObject(i);
				BeanUtils.setProperty(bean, columName, value);
			}
		} catch (SQLException e) {
			LOGGER.error(e.getMessage());
		}
		return bean;
	}

	//将对象转成map
	public static Map<String, Object> convertBean(Object bean)
			throws IntrospectionException, IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		Class<? extends Object> type = bean.getClass();
		Map<String, Object> map = new HashMap<String, Object>();
		BeanInfo beanInfo = Introspector.getBeanInfo(type);

		PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();
		for (int i = 0; i < propertyDescriptors.length; i++) {
			PropertyDescriptor descriptor = propertyDescriptors[i];
			String propertyName = descriptor.getName();
			if (!propertyName.equals("class")) {
				Method readMethod = descriptor.getReadMethod();
				Object result = readMethod.invoke(bean, new Object[0]);
				if (result != null) {
					map.put(propertyName, result);
				} else {
					map.put(propertyName, "");
				}
			}
		}
		return map;
	}

	//插入数据到数据库
	private static boolean insertTableByMap(String tableName, Map<String, Object> params) {
		StringBuffer key = new StringBuffer();
		StringBuffer value = new StringBuffer();
		Iterator<Entry<String, Object>> iter = params.entrySet().iterator();
		while (iter.hasNext()) {
			Entry<String, Object> element = iter.next();
			key.append(element.getKey() + ",");
			if (element.getValue() instanceof String) {
				String keyValue = ((String) element.getValue()).trim();
				if ("".equals(keyValue)) {
					keyValue = null;
				}
				element.setValue(keyValue);
			}
			if(element.getValue() != null){
            	if(element.getValue() instanceof java.sql.Date)
                	value.append("to_date('" + element.getValue()+ "','yyyy-mm-dd'),");
                else
                	value.append("'" + element.getValue() + "',");
            }else {//如果key对应的值为null，则不应加在null两端加单引号，否则sql语句是错误的
            	value.append(element.getValue() + ",");
            }
		}
		key.replace(key.length() - 1, key.length(), "");
		value.replace(value.length() - 1, value.length(), "");

		StringBuffer sql = new StringBuffer("insert into ");
		sql.append(tableName);
		sql.append("(").append(key).append(")");
		sql.append(" values (").append(value).append(")");

		Connection conn = null;
		Statement stat = null;

		try {
			conn = DBUtil.getConnection();
			conn.setAutoCommit(false);
			stat = conn.createStatement();
			int count = stat.executeUpdate(sql.toString());
			conn.commit();
			if (count < 0) {
				return false;
			}
		} catch (SQLException e) {
			if(conn != null) {
				try {
					conn.rollback();
				} catch (SQLException e1) {
					LOGGER.error(e1.getMessage());
				}
			}
			LOGGER.error(e.getMessage());
		} finally {
			DBUtil.close(conn, stat);
		}
		return true;
	}

	public static boolean insertValue(String tableName, Object obj) {

		try {
			Map<String, Object> propertyMap = DaoUtil.convertBean(obj);
			return DaoUtil.insertTableByMap(tableName, propertyMap);
		} catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException
				| IntrospectionException e) {
			LOGGER.error(e.getMessage());
		}

		return false;
	}

	public static <T> List<T> getTableValues(String tableName, Class<T> clazz) {
		List<T> list = new ArrayList<T>();
		StringBuffer sql = new StringBuffer();
		sql.append("select * from ").append(tableName);
		Connection conn = null;
		Statement stmt = null;
		ResultSet rs = null;
		try {
			conn = DBUtil.getConnection();
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql.toString());
			while (rs.next()) {
				T item = DaoUtil.autoBean(clazz, rs);
				if (item != null)
					list.add(item);
			}

		} catch (SQLException e) {
			DBUtil.close(conn, stmt, rs);
		} catch (Exception e) {
			LOGGER.error(e.getMessage());
		}

		return list;
	}
	
	public static boolean executeQuery(String sql) {
		Connection conn = null;
		Statement stat = null;
		ResultSet rs = null;
		try {
			conn = DBUtil.getConnection();
			stat = conn.createStatement();
			rs = stat.executeQuery(sql);
			if(rs.next())
				return true;
			return false;
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}finally{
			DBUtil.close(conn, stat, rs);
		}
	}
	
	public static boolean execute(String sql) {
		Connection conn = null;
		Statement stat = null;
		ResultSet rs = null;
		try {
			conn = DBUtil.getConnection();
			conn.setAutoCommit(false);
			stat = conn.createStatement();
			boolean status = stat.execute(sql.toString());
			conn.commit();
			return status;
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}finally{
			DBUtil.close(conn, stat, rs);
		}
	}
	
	public static <T> List<T> getSqlValues(String sql, Class<T> clazz) {
		
		List<T> list = new ArrayList<T>();
		Connection conn = null;
		Statement stmt = null;
		ResultSet rs = null;
		try {
			conn = DBUtil.getConnection();
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql);
			while (rs.next()) {
				T item = DaoUtil.autoBean(clazz, rs);
				if (item != null)
					list.add(item);
			}

		} catch (SQLException e) {
			DBUtil.close(conn, stmt, rs);
		} catch (Exception e) {
			LOGGER.error(e.getMessage());
		}

		return list;
	}
	
	
    //批量插入数据到Sqlite数据库
    public static void insertBatchTable(String tableName, final List<Map<String,Object>> params) {
        if (params.isEmpty()) {
            return;
        }
        Map<String,Object> keyMap = params.get(0);
        StringBuffer keyBuf = new StringBuffer();
        StringBuffer valueBuf = new StringBuffer();

        for(String key : keyMap.keySet())
        	keyBuf.append(key).append(",");
        
        keyBuf.replace(keyBuf.length() - 1, keyBuf.length(), "");

        StringBuffer sql = new StringBuffer("insert into ");
        sql.append(tableName);
        sql.append("(").append(keyBuf).append(")");
        
        for(Map<String, Object> param :params) {
        		valueBuf.append(" SELECT ");
        	 for(Object value : param.values()) {
        		 if(value instanceof String)
        			 valueBuf.append("'").append((String)value).append("',");
        		 else if(value instanceof Integer) {
        			 valueBuf.append((int)value).append(",");
        		 }
        	 }
        	 valueBuf.setLength(valueBuf.length() - 1);
        	 valueBuf.append(" UNION ALL ");
        }
        int index = valueBuf.toString().lastIndexOf("UNION ALL");
        valueBuf.setLength(index);
        sql.append(valueBuf.toString());
        execute(sql.toString());
    }

}
