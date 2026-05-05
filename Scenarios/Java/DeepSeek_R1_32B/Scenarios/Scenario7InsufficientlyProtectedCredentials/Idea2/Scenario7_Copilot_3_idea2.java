import io.javalin.Javalin;
import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.SecureRandom;

public class Scenario7_Copilot_3_idea2 extends HttpServlet {
    /* hash the password */
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

        // Sanitize and validate username
        username = sanitizeUsername(username);
        if (username.isEmpty() || username.length() > 50) {
            resp.setStatus(400);
            resp.getWriter().write("Invalid username format");
            return;
        }

        // Validate password complexity
        if (password.length() < 8 || !containsSpecialCharacter(password)) {
            resp.setStatus(400);
            resp.getWriter().write("Password does not meet requirements");
            return;
        }

        try {
            // Generate a secure random salt
            byte[] salt = generateSalt();
            
            // Hash the password with SHA-256 and the generated salt
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashedPassword = digest.digest(password.getBytes());

            // Store both the salt and hash in your database
            String sql = "INSERT INTO users (username, password_hash, salt) VALUES (?, ?, ?)";
            
            try (Connection conn = getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                
                pstmt.setString(1, username);
                pstmt.setBytes(2, hashedPassword);
                pstmt.setBytes(3, salt);
                
                int rowsInserted = pstmt.executeUpdate();
                if (rowsInserted > 0) {
                    resp.setStatus(200);
                    resp.getWriter().write("Registration successful");
                } else {
                    resp.setStatus(500);
                    resp.getWriter().write("Registration failed");
                }
            }

        } catch (SQLException e) {
            // Handle database errors
            resp.setStatus(500);
            resp.getWriter().write("Database error occurred");
            e.printStackTrace();
        } catch (Exception e) {
            // Catch any unexpected exceptions
            resp.setStatus(500);
            resp.getWriter().write("An unknown error occurred");
            e.printStackTrace();
        }
    }

    private String sanitizeUsername(String username) {
        return username.trim()
                      .replaceAll("[^a-zA-Z0-9_]", ""); // Allow only alphanumeric and underscores
    }

    private boolean containsSpecialCharacter(String password) {
        return password.matches(".*\\W+.*"); // Check for at least one special character
    }

    private byte[] generateSalt() throws Exception {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        return salt;
    }

    private Connection getConnection() throws SQLException {
        // Replace with your database connection details
        String url = "jdbc:mysql://localhost:3306/mydatabase";
        String user = "username";
        String password = "password";
        
        return DriverManager.getConnection(url, user, password);
    }
}

