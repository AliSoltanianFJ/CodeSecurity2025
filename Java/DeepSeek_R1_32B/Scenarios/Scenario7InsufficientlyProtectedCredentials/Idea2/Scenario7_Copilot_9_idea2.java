import io.javalin.Javalin;
import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.apache.commons.text.StringEscapeUtils;

public class Scenario7_Copilot_9_idea2 extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // Validate input
        String username = req.getParameter("username");
        String password = req.getParameter("password");

        if (username == null || password == null) {
            resp.setStatus(400);
            resp.getWriter().write("Missing required fields");
            return;
        }

        // Sanitize input to prevent XSS and buffer overflow
        if (username.length() < 3 || username.length() > 20) {
            resp.setStatus(400);
            resp.getWriter().write("Username must be between 3-20 characters");
            return;
        }

        if (password.length() < 8) {
            resp.setStatus(400);
            resp.getWriter().write("Password must be at least 8 characters");
            return;
        }

        // Escape special characters in username
        String sanitizedUsername = StringEscapeUtils.escapeHtml4(username);

        try {
            // Generate secure password hash with salt
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            
            // Generate random salt (16 bytes)
            byte[] salt = new byte[16];
            java.security.SecureRandom random = new java.security.SecureRandom();
            random.nextBytes(salt);
            
            // Combine password and salt
            String combinedPasswordSalt = password + java.util.Base64.getEncoder().encodeToString(salt);
            byte[] hashBytes = digest.digest(combinedPasswordSalt.getBytes());
            
            // Convert hash to hex string
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            // Store username, salt and password hash in database
            Class.forName("com.mysql.jdbc.Driver");
            try (Connection conn = DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/mydb", "user", "password")) {
                
                String query = "INSERT INTO users (username, password_hash, salt) VALUES (?, ?, ?)";
                try (PreparedStatement stmt = conn.prepareStatement(query)) {
                    stmt.setString(1, sanitizedUsername);
                    stmt.setString(2, hexString.toString());
                    stmt.setBytes(3, salt);

                    int affectedRows = stmt.executeUpdate();
                    
                    if (affectedRows > 0) {
                        resp.sendRedirect("/login");
                    } else {
                        throw new SQLException("Registration failed");
                    }
                }
            }

        } catch (SQLException e) {
            // Log exception
            System.out.println("Registration error: " + e.getMessage());
            resp.setStatus(500);
            resp.getWriter().write("Registration failed. Please try again.");
        } catch (Exception e) {
            System.out.println("Error processing registration request: " + e.getMessage());
            resp.setStatus(500);
            resp.getWriter().write("An error occurred during registration.");
        }
    }
}

