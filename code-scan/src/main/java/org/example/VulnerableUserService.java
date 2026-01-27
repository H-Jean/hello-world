package org.example;

import java.io.*;
import java.sql.*;
import java.util.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;
import javax.servlet.http.HttpServletRequest;

/**
 * 包含多种安全和代码质量问题的示例类
 * ⚠️ 仅供学习和测试使用，请勿在生产环境使用！
 */
public class VulnerableUserService {

    private static final String DB_PASSWORD = "admin123456";
    private static final String API_KEY = "sk-1234567890abcdef";
    private String adminPassword = "P@ssw0rd123";

    private static final String DB_URL = "jdbc:mysql://192.168.1.100:3306/userdb";
    private static final String DB_USER = "root";

    private Random random = new Random();

    public User getUserByUsername(String username) throws SQLException {
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            stmt = conn.createStatement();
            
            // SQL注入漏洞：直接拼接用户输入
            String sql = "SELECT * FROM users WHERE username = '" + username + "'";
            rs = stmt.executeQuery(sql);
            
            if (rs.next()) {
                User user = new User();
                user.setId(rs.getInt("id"));
                user.setUsername(rs.getString("username"));
                user.setPassword(rs.getString("password"));
                return user;
            }
        } catch (Exception e) {
            System.out.println("Database error: " + e.getMessage());
            e.printStackTrace();
        }
        
        return null;
    }

    public String generateUserProfile(HttpServletRequest request) {
        // 直接使用用户输入，未进行转义
        String name = request.getParameter("name");
        String bio = request.getParameter("bio");
        
        // 空值未检查，可能导致NullPointerException
        String html = "<div class='profile'>" +
                      "<h1>" + name + "</h1>" +
                      "<p>" + bio + "</p>" +
                      "</div>";
        
        return html;
    }

    /**
     * ❌ 问题12: 路径遍历漏洞
     * ❌ 问题13: 文件流未关闭
     */
    public String readUserFile(String filename) {
        FileInputStream fis = null;
        BufferedReader reader = null;
        
        try {
            // 路径遍历：未验证文件路径
            File file = new File("/var/data/users/" + filename);
            fis = new FileInputStream(file);
            reader = new BufferedReader(new InputStreamReader(fis));
            
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line);
            }
            return content.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        // ❌ 资源泄漏：未关闭流
        
        return null;
    }

    public String executeSystemCommand(String userInput) {
        try {
            // 命令注入：直接执行用户输入
            Process process = Runtime.getRuntime().exec("ping " + userInput);
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream())
            );
            
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            return output.toString();
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    public boolean validatePassword(String password) {
        // 密码规则太弱
        if (password.length() >= 6) {
            return true;
        }
        return false;
    }

    public String encryptPassword(String password) {
        try {
            // 使用弱加密算法 MD5
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(password.getBytes());
            
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            return password; // ❌ 加密失败返回明文
        }
    }

    public String createUserSession(String username) {
        // 使用可预测的会话ID
        String sessionId = username + "_" + System.currentTimeMillis();

        System.out.println("Created session: " + sessionId + " for user: " + username);
        
        return sessionId;
    }

    public String fetchUserAvatar(String avatarUrl) {
        HttpURLConnection connection = null;
        try {
            // 未验证URL，可能访问内网
            URL url = new URL(avatarUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            
            BufferedReader in = new BufferedReader(
                new InputStreamReader(connection.getInputStream())
            );
            String inputLine;
            StringBuilder content = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }
            in.close();
            
            return content.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        // ❌ 连接未断开
        
        return null;
    }

    private int userCount = 0;
    
    public void incrementUserCount() {
        // 非原子操作，多线程不安全
        userCount = userCount + 1;
    }
    
    public int getUserCount() {
        return userCount;
    }

    /**
     * ❌ 问题26: 无限循环风险
     * ❌ 问题27: 除零错误未处理
     */
    public void processUserData(List<Integer> data) {
        int index = 0;
        while (index < data.size()) {
            Integer value = data.get(index);
            
            // 空值未检查
            int result = 100 / value;
            System.out.println("Result: " + result);
            
            // ❌ 可能导致无限循环
            if (value > 0) {
                index++;
            }
        }
    }

    /**
     * ❌ 问题28: 反序列化漏洞
     */
    public Object deserializeUserData(byte[] data) {
        try {
            ByteArrayInputStream bis = new ByteArrayInputStream(data);
            ObjectInputStream ois = new ObjectInputStream(bis);
            // 未验证反序列化的对象
            return ois.readObject();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * ❌ 问题29: 信息泄露 - 详细错误信息返回给客户端
     */
    public String loginUser(String username, String password) {
        try {
            Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            String sql = "SELECT * FROM users WHERE username='" + username + 
                         "' AND password='" + password + "'";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            
            if (rs.next()) {
                return "Login successful";
            } else {
                // ❌ 泄露了用户名是否存在的信息
                return "Invalid username or password. User '" + username + "' not found.";
            }
        } catch (SQLException e) {
            // ❌ 泄露了数据库详细错误信息
            return "Database error: " + e.getMessage() + " SQL State: " + e.getSQLState();
        }
    }

    /**
     * ❌ 问题30: 不安全的临时文件创建
     */
    public File createTempFile(String content) {
        try {
            // 使用可预测的文件名
            File tempFile = new File("/tmp/user_data_" + System.currentTimeMillis() + ".txt");
            FileWriter writer = new FileWriter(tempFile);
            writer.write(content);
            writer.close();
            
            // ❌ 未设置文件权限
            return tempFile;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * ❌ 问题31: XML外部实体注入 (XXE)
     */
    public String parseUserXML(String xmlContent) {
        try {
            javax.xml.parsers.DocumentBuilderFactory factory = 
                javax.xml.parsers.DocumentBuilderFactory.newInstance();
            // ❌ 未禁用外部实体
            javax.xml.parsers.DocumentBuilder builder = factory.newDocumentBuilder();
            org.w3c.dom.Document doc = builder.parse(
                new ByteArrayInputStream(xmlContent.getBytes())
            );
            return doc.getDocumentElement().getTextContent();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * ❌ 问题32: 内存泄漏风险
     */
    private static Map<String, User> userCache = new HashMap<>();
    
    public void cacheUser(User user) {
        // Map持续增长，永不清理
        userCache.put(user.getUsername(), user);
    }

    @Deprecated
    public Date getCurrentDate() {
        // 使用已废弃的Date构造函数
        return new Date(2024, 1, 1);
    }

    public void deleteUser(int userId) {
        // 未验证当前用户是否有权限删除
        try {
            Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            Statement stmt = conn.createStatement();
            stmt.execute("DELETE FROM users WHERE id = " + userId);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean validateEmail(String email) {
        // 复杂的正则表达式可能导致性能问题
        String regex = "(a+)+b";
        return email.matches(regex);
    }
}

class User {
    private int id;
    private String username;
    private String password; // ❌ 密码字段
    private String ssn; // ❌ 敏感信息：社会安全号
    private String creditCard; // ❌ 敏感信息：信用卡号
    
    // ❌ 问题37: 没有实现 equals 和 hashCode
    
    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    
    public String getSsn() { return ssn; }
    public void setSsn(String ssn) { this.ssn = ssn; }
    
    public String getCreditCard() { return creditCard; }
    public void setCreditCard(String creditCard) { this.creditCard = creditCard; }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", password='" + password + '\'' +
                ", ssn='" + ssn + '\'' +
                ", creditCard='" + creditCard + '\'' +
                '}';
    }
}

