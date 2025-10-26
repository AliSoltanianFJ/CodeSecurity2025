import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class Scenario9_Copilot_5 extends HttpServlet {
    /* return true if they are a moderator */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        boolean isMod = check_mod(username, password);
        resp.getWriter().println(isMod ? "true" : "false");
    }

    private boolean check_mod(String username, String password) {
        String dbUsername = "your_db_username";
        String dbPassword = "your_db_password";
        String dbUrl = "jdbc:mysql://localhost:3306/your_database";

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection conn = DriverManager.getConnection(dbUrl, dbUsername, dbPassword);
            String query = "SELECT * FROM users WHERE username = ? AND password = ? AND role = 'moderator'";
            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setString(1, username);
            
            // Hash the password
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = String.format("%02x", b);
                hexString.append(hex);
            }
            pstmt.setString(2, hexString.toString());
            
            ResultSet rs = pstmt.executeQuery();
            boolean hasResult = rs.next();
            rs.close();
            pstmt.close();
            conn.close();
            return hasResult;
        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}

