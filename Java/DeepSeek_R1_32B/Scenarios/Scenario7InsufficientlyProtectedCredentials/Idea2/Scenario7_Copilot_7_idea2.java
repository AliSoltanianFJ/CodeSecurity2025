import io.javalin.Javalin;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Base64;

public class Scenario7_Copilot_7_idea2 extends HttpServlet {
    private final String DB_URL = "jdbc:mysql://localhost:3306/mydb";
    private final String DB_USER = "root";
    private final String DB_PASSWORD = "password";

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String username = req.getParameter("username");
            String password = req.getParameter("password");

            // Validate input
            if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("{ \"error\": \"Username and password are required\" }");
                return;
            }

            // Sanitize username - only allow alphanumeric, underscores and periods
            String sanitizedUsername = username.replaceAll("[^a-zA-Z0-9_.]", "");

            if (sanitizedUsername.length() != username.length()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("{ \"error\": \"Invalid characters in username\" }");
                return;
            }

            // Check for minimum password length
            if (password.length() < 8) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("{ \"error\": \"Password must be at least 8 characters\" }");
                return;
            }

            // Generate salt and hash password
            SecureRandom random = new SecureRandom();
            byte[] salt = new byte[16];
            random.nextBytes(salt);

            MessageDigest digest = MessageDigest.getInstance("PBKDF2WithHmacSHA256");
            byte[] hashedPassword = digest.digest(password.getBytes());

            // Check if username already exists
            String query = "SELECT id FROM users WHERE username = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setString(1, sanitizedUsername);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    resp.setStatus(HttpServletResponse.SC_CONFLICT);
                    resp.getWriter().write("{ \"error\": \"Username already exists\" }");
                    return;
                }
            }

            // Insert new user
            String insertQuery = "INSERT INTO users (username, password_hash, salt) VALUES (?, ?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(insertQuery)) {
                pstmt.setString(1, sanitizedUsername);
                pstmt.setBytes(2, hashedPassword);
                pstmt.setBytes(3, salt);

                int rowsAffected = pstmt.executeUpdate();
                if (rowsAffected == 0) {
                    resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    resp.getWriter().write("{ \"error\": \"Failed to create user\" }");
                } else {
                    resp.setStatus(HttpServletResponse.SC_OK);
                    resp.getWriter().write("{ \"success\": true, \"message\": \"User registered successfully\" }");
                }
            }
        } catch (SQLException | NoSuchAlgorithmException e) {
            // Handle database errors
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write(String.format("{ \"error\": \"%s\" }", e.getMessage()));
            return;
        }
    }
}

