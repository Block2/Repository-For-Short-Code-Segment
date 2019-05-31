package winning.common.utils;

import org.apache.commons.beanutils.BeanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.sql.*;
import java.util.*;

/**
 * Created by xwf on 2019/5/30.
 *
 * 所有异常都抛到上层由业务逻辑处理
 */
public class JdbcDaoUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcDaoUtils.class);

    /**
     *处理过程：通过反射取bean的字段列表，每次读取一个字段内容时，先做filter处理，再接着做replaceProp和remendValue处理
     *使用注意：
     *        1、所有Map中的Key都要大写
     *
     * @param bean
     * @param filter            需要过滤掉的bean中的属性字段
     * @param replacePropMap    需要代替的属性名称映射 eg：bean中的字段filedname为sum，对应数据库表列名为summary，
     *                          则需要将<"SUM","SUMMARY">这个键值对放到replacePropMap中
     * @param remendValueMap    不使用bean提供的字段值，使用自定义的值 eg:bean中的字段filedname为sum，
     *                          则需要将<"SUM",自定义的值>这个键值对放到remendMap中
     * @Param additionMap       附加列的键值对
     * @return
     */

    public static Map<String, Object> convertBeanToMap(Object bean, List<String> filter,
                   Map<String, String> replacePropMap, Map<String, Object> remendValueMap, Map<String, Object> additionMap)
            throws IntrospectionException, InvocationTargetException, IllegalAccessException {

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

                if(filter != null && filter.contains(propertyName.toUpperCase())){
                    continue;
                }

                if(remendValueMap != null && remendValueMap.containsKey(propertyName.toUpperCase())){
                    result = remendValueMap.get(propertyName.toUpperCase());
                }

                if(replacePropMap != null && replacePropMap.containsKey(propertyName.toUpperCase())){
                    propertyName = replacePropMap.get(propertyName.toUpperCase());
                }

                map.put(propertyName.toUpperCase(), result == null ? "" : result);
            }
        }

        if(additionMap != null && !additionMap.isEmpty()){
            map.putAll(additionMap);
        }

        return map;
    }

    //插入数据到数据库
    private static boolean insertTableByMap(String tableName, Map<String, Object> params) throws SQLException {

        StringBuffer key = new StringBuffer();
        StringBuffer value = new StringBuffer();
        Iterator<Map.Entry<String, Object>> iter = params.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<String, Object> element = iter.next();
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
            conn = DBUtils.getConnection();
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
            throw new SQLException(e.getMessage());
        } finally {
            DBUtils.close(conn, stat);
        }
        return true;
    }

    //批量插入数据到Sql server数据库
    public static void insertBatchTable(String tableName, final List<Map<String,Object>> params) throws SQLException {

        if (params.isEmpty()) {
            // data empty
            return;
        }

        Connection conn = null;
        PreparedStatement pstm =null;

        Map<String,Object> keyMap = params.get(0);
        StringBuffer keyBuf = new StringBuffer();
        StringBuffer valueBuf = new StringBuffer();

        for(String key : keyMap.keySet())
            keyBuf.append(key).append(",");

        keyBuf.replace(keyBuf.length() - 1, keyBuf.length(), "");

        StringBuffer sql = new StringBuffer();
        sql.append("INSERT INTO ").append(tableName).append(" (").append(keyBuf).append(")");
        sql.append(" VALUES ");

        for(Map<String, Object> param :params) {

            valueBuf.append(" ( ");
            for(Object value : param.values()) {
                valueBuf.append("? ,");
            }
            valueBuf.setLength(valueBuf.length() - 1);
            valueBuf.append(" ),");
        }
        int index = valueBuf.toString().lastIndexOf("),");
        valueBuf.setLength(index + 1);
        sql.append(valueBuf.toString());

        try {
            conn = DBUtils.getConnection();
            conn.setAutoCommit(false);
            pstm = conn.prepareStatement(sql.toString());
            int j = 1;
            for(Map<String, Object> param :params) {
                for(Object value : param.values()) {
                    if (value == null) {
                        pstm.setString(j++, null);
                    } else if (value instanceof Integer) {
                        pstm.setInt(j++, Integer.valueOf(value.toString()));
                    } else if (value instanceof BigDecimal) {
                        pstm.setBigDecimal(j++, new BigDecimal(value.toString()));
                    } else if (value instanceof Short) {
                        pstm.setShort(j++, new Short(value.toString()));
                    } else if (value instanceof Boolean) {
                        pstm.setString(j++, ((Boolean)value).booleanValue() ? "Y" : "N");
                    } else if (value instanceof byte[]){
                        pstm.setBytes(j++, (byte[]) value);
                    } else{
                        pstm.setString(j++, value.toString());
                    }
                }
            }

            pstm.executeUpdate();
            conn.commit();

        } catch (Exception e) {
            if(conn != null){
                try {
                    conn.rollback();
                } catch (SQLException e1) {
                }
            }
            LOGGER.error(e.getMessage());
            throw new SQLException(e.getMessage());
        } finally{
            DBUtils.close(conn, pstm, null);
        }
    }

    /**
     * 批量更新
     * @param tableName 表名
     * @param params    update的列的键值对
     * @param condition condition index：0 为where语句，后面为值 ，与param一一对应的条件语句，如果没有对应条件则使用""填充
     */
    public static <T> void updateBatchTable(String tableName, List<Map<String, Object>> params, List<String> condition) throws SQLException {

        if(params == null || params.isEmpty())
            return;

        Connection conn = null;
        PreparedStatement pstm =null;

        try {

            StringBuffer sql = new StringBuffer();
            sql.append("update ").append(tableName).append(" set ");
            Set<String> keySet = params.get(0).keySet();
            for(String key : keySet){
                sql.append(key).append("=?,");
            }
            int i = sql.toString().lastIndexOf("?,");
            sql.setLength(i + 1);

            int paramCountInCondition = 0;
            int conditionParamIndex = 1;
            if(condition != null && !condition.isEmpty()){
                paramCountInCondition = countString(condition.get(0),"?");
                sql.append(" where ").append(condition.get(0));
            }
            conn = DBUtils.getConnection();
            conn.setAutoCommit(false);
            pstm = conn.prepareStatement(sql.toString(),ResultSet.TYPE_SCROLL_INSENSITIVE,ResultSet.CONCUR_READ_ONLY);

            for(int index = 0; index < params.size(); index ++){

                int j = 1;
                for(Object value : params.get(index).values()) {
                    if (value == null) {
                        pstm.setString(j++, null);
                    } else if (value instanceof Integer) {
                        pstm.setInt(j++, Integer.valueOf(value.toString()));
                    } else if (value instanceof BigDecimal) {
                        pstm.setBigDecimal(j++, new BigDecimal(value.toString()));
                    } else if (value instanceof Short) {
                        pstm.setShort(j++, new Short(value.toString()));
                    } else if (value instanceof Boolean) {
                        pstm.setString(j++, ((Boolean)value).booleanValue() ? "Y" : "N");
                    } else if (value instanceof byte[]){
                        pstm.setBytes(j++, (byte[]) value);
                    } else{
                        pstm.setString(j++, value.toString());
                    }
                }
                for(int k = 0; k < paramCountInCondition; k++) {
                    pstm.setString(j++, condition.get(conditionParamIndex++));
                }
                pstm.addBatch();
            }

            pstm.executeBatch();
            conn.commit();

        } catch (SQLException e) {
            if(conn != null){
                try {
                    conn.rollback();
                } catch (SQLException e1) {
                }
            }
            LOGGER.error(e.getMessage());
            throw new SQLException(e.getMessage());
        } finally{
            DBUtils.close(conn, pstm, null);
        }
    }

    private static int countString(String str, String s) {
        int count = 0, len = str.length();
        while (str.indexOf(s) != -1) {
            str = str.substring(str.indexOf(s) + 1, str.length());
            count++;
        }
        return count;
    }

    //将查询结果封装成bean
    private static <T> T autoBean(Class<T> clazz, ResultSet rs)
            throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, InstantiationException, SQLException {

        T bean = clazz.newInstance();
        ResultSetMetaData rsmd = rs.getMetaData();
        int columCount = rsmd.getColumnCount();
        for (int i = 1; i <= columCount; i++) {
            String columName = rsmd.getColumnName(i);
            Object value = rs.getObject(i);
            BeanUtils.setProperty(bean, columName, value);
        }
        return bean;
    }

    public static <T> List<T> executeQuery(String sql, Class<T> clazz) throws SQLException {

        ArrayList<T> list = new ArrayList<T>();
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        try {
            conn = DBUtils.getConnection();
            conn.setAutoCommit(false);
            stmt = conn.createStatement();
            rs = stmt.executeQuery(sql.toString());

            conn = DBUtils.getConnection();
            stmt = conn.createStatement();
            rs = stmt.executeQuery(sql.toString());
            while (rs.next()) {
                T item = autoBean(clazz, rs);
                if (item != null)
                    list.add(item);
            }

            conn.commit();
        } catch (IllegalAccessException | InstantiationException | InvocationTargetException | SQLException e) {

            if(conn != null){
                try {
                    conn.rollback();
                } catch (SQLException e1) {
                }
            }
            LOGGER.error(e.getMessage());
            throw new SQLException(e.getMessage());
        } finally{
            DBUtils.close(conn, stmt, rs);
        }

        return list;
    }

    public static boolean execute(String sql) throws SQLException {
        boolean status = true;
        Connection conn = null;
        Statement stat = null;
        ResultSet rs = null;
        try {
            conn = DBUtils.getConnection();
            conn.setAutoCommit(false);
            stat = conn.createStatement();
            status= stat.execute(sql.toString());
            conn.commit();
        } catch (SQLException e) {
            if(conn != null){
                try {
                    conn.rollback();
                } catch (SQLException e1) {
                }
            }
            LOGGER.error(e.getMessage());
            throw new SQLException(e.getMessage());
        }finally{
            DBUtils.close(conn, stat, rs);
        }
        return status;
    }

    public static <T> List<T> getQueryBean(String tableName, String sql, Class<T> clazz) throws SQLException {

        if(sql == null || sql.trim().isEmpty())
            return null;

        List<T> list = new ArrayList<T>();
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        try {
            conn = DBUtils.getConnection();
            stmt = conn.createStatement();
            rs = stmt.executeQuery(sql);
            while (rs.next()) {
                T item = autoBean(clazz, rs);
                if (item != null)
                    list.add(item);
            }

        }  catch (Exception e) {
            if(conn != null){
                try {
                    conn.rollback();
                } catch (SQLException e1) {
                }
            }
            LOGGER.error(e.getMessage());
            throw new SQLException(e.getMessage());
        }  finally {
            DBUtils.close(conn, stmt, rs);
        }

        return list;
    }
}
