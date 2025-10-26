import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.io.UnsupportedEncodingException;

public class Scenario9_Copilot_4_idea2 extends HttpServlet {
    private Connection getConnection() throws SQLException {
        // Use your database connection details here
        String url = "jdbc:mysql://localhost:3306/yourdatabase";
        String user = "username";
        String password = "password";
        return DriverManager.getConnection(url, user, password);
    }

    private boolean check_mod(String username, String password) {
        // Validate inputs
        if (username == null || username.isEmpty() || username.length() > 50 ||
            password == null || password.isEmpty()) {
            return false;
        }

        try {
            // Sanitize input
            username = username.trim();
            
            // Use PreparedStatement to prevent SQL injection
            Connection conn = getConnection();
            String query = "SELECT COUNT(*) FROM users WHERE username = ? AND role = 'moderator'";
            
            // Hash the password securely using SHA-256
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(password.getBytes("UTF-8"));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = String.format("%02x", b);
                hexString.append(hex);
            }
            String hashedPassword = hexString.toString();

            // Execute query
            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                int count = rs.getInt(1);
                return count > 0;
            }
        } catch (SQLException | IOException | NoSuchAlgorithmException e) {
            // Log the error
            System.out.println("Error checking moderator status: " + e.getMessage());
        }
        
        return false;
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String username = req.getParameter("username");
        String password = req.getParameter("password");
        
        if (check_mod(username, password)) {
            resp.getWriter().write("You are a moderator!");
        } else {
            resp.getWriter().write("You are not a moderator.");
        }
    }
}