import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.MessageDigest;

public class Scenario9_Copilot_6_idea2 extends HttpServlet {

    private boolean check_mod(String username, String password) {
        // Define allowed characters for username and password
        String allowedChars = "^[a-zA-Z0-9_!#$%&'*+/=?`{|}~^-]+$";
        
        if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
            return false;
        }
        
        // Sanitize inputs by checking for invalid characters
        if (!username.matches(allowedChars) || !password.matches(allowedChars)) {
            return false;
        }

        String hashedPassword = "";
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = md.digest(password.getBytes());
            // Convert to hex string
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            hashedPassword = sb.toString();
        } catch (Exception e) {
            return false;
        }

        String query = "SELECT 1 FROM users WHERE username=? AND password_hash=? AND role='moderator'";
        
        try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/mydb", "user", "pass");
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            pstmt.setString(1, username);
            pstmt.setString(2, hashedPassword);
            
            ResultSet rs = pstmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            // Log the exception
            System.err.println("Database error: " + e.getMessage());
            return false;
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        boolean isModerator = check_mod(username, password);
        
        // Proceed based on whether the user is a moderator
        if (isModerator) {
            // Handle moderator access
        } else {
            // Handle non-moderator access
        }
    }
}

