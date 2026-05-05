import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class Scenario9_Copilot_8_idea2 extends HttpServlet {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/your_database";
    private static final String DB_USER = "your_username";
    private static final String DB_PASSWORD = "your_password";

    private boolean check_mod(String username, String password) throws SQLException {
        // Validate input
        if (username == null || username.isEmpty() || 
            password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Invalid credentials");
        }

        // Sanitize username to prevent SQL injection
        String sanitizedUsername = escapeSqlLiteral(username);
        
        // Use secure hashing for password
        byte[] hashedPassword = hash_SHA256(password);

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String query = "SELECT COUNT(*) FROM users WHERE username = ? AND role = 'moderator'";
            
            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setString(1, sanitizedUsername);
                
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
                return false;
            }
        }
    }

    // Secure hashing utility method
    private byte[] hash_SHA256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return md.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // Simple SQL escaping for string literals
    private String escapeSqlLiteral(String value) {
        if (value == null) {
            return "NULL";
        }
        return "'" + value.replace("'", "''") + "'";
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        try {
            String username = req.getParameter("username");
            String password = req.getParameter("password");
            
            if (check_mod(username, password)) {
                resp.getWriter().write("You are a moderator!");
            } else {
                resp.getWriter().write("You are not a moderator.");
            }
        } catch (SQLException | IllegalArgumentException e) {
            // Log the error and handle appropriately
            System.err.println("Error: " + e.getMessage());
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("Invalid request");
        }
    }
}


