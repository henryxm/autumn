package cn.org.autumn.table.utils;

import java.sql.*;

public class MysqlTool {
    Connection connection;
    Statement statement;
    String host;
    int port;

    public MysqlTool(String host, int port) throws ClassNotFoundException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            Class.forName("com.mysql.jdbc.Driver");
        }
        this.host = host;
        this.port = port;
    }

    public void connect(String root, String password) throws SQLException {
        String url = "jdbc:mysql://" + host + ":" + port + "/mysql?serverTimezone=Asia/Shanghai&useSSL=false&allowMultiQueries=true&useUnicode=true&characterEncoding=UTF-8&allowPublicKeyRetrieval=true";
        connection = DriverManager.getConnection(url, root, password);
        statement = connection.createStatement();
    }

    //创建一个数据库
    public boolean createDatabase(String database) throws SQLException {
        statement.executeUpdate("DROP DATABASE IF EXISTS " + database);
        int result = statement.executeUpdate("CREATE DATABASE " + database);
        return result == 1;
    }

    //创建一个用户
    public void createUser(String username, String password) throws SQLException {
        statement.executeUpdate("DROP USER IF EXISTS " + username + "@'%'");
        statement.executeUpdate("create user " + username + "@'%' identified by '" + password + "'");
    }

    //授权用户
    public void grantPrivileges(String username, String database) throws SQLException {
        statement.executeUpdate("grant all privileges on " + database + ".* to " + username);
    }

    public void close() throws SQLException {
        statement.close();
        connection.close();
    }

    /**
     * 使用mysql高权限账号创建一个数据库，用户，并授权用户对该数据库的权限
     *
     * @param host     数据库服务器
     * @param port     数据库端口
     * @param root     root 用户
     * @param rootPass root 用户密码
     * @param database 要创建的数据库
     * @param username 数据库的用户名
     * @param password 数据库的密码
     * @return 创建成功返回true, 否则返回false
     */
    public static boolean create(String host, int port, String root, String rootPass, String database, String username, String password) {
        try {
            MysqlTool mysqlTool = new MysqlTool(host, port);
            mysqlTool.connect(root, rootPass);
            boolean r = mysqlTool.createDatabase(database);
            if (r) {
                mysqlTool.createUser(username, password);
                mysqlTool.grantPrivileges(username, database);
            }
            mysqlTool.close();
            return r;
        } catch (Exception e) {
            return false;
        }
    }
}