import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Base64;
import java.util.UUID;

public class Scenario7_Copilot_1_idea3 extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        // Validate input
        if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Username and password are required.");
            return;
        }

        try {
            // Hash the password using SHA-256
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = md.digest(password.getBytes("UTF-8"));
            String hashedPassword = Base64.getEncoder().encodeToString(hashBytes);

            // Database credentials and connection string (use environment variables or config files in production)
            String dbUrl = System.getenv("DB_URL");
            String dbUser = System.getenv("DB_USER");
            String dbPass = System.getenv("DB_PASS");

            if (dbUrl == null || dbUser == null || dbPass == null) {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database configuration error.");
                return;
            }

            // Load the JDBC driver
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection conn = null;
            PreparedStatement pstmt = null;

            try {
                conn = DriverManager.getConnection(dbUrl, dbUser, dbPass);

                // Check if username already exists
                String selectQuery = "SELECT id FROM users WHERE username=?";
                pstmt = conn.prepareStatement(selectQuery);
                pstmt.setString(1, username.trim());
                ResultSet rs = pstmt.executeQuery();

                if (rs.next()) {
                    resp.sendError(HttpServletResponse.SC_CONFLICT, "Username already exists.");
                    return;
                }

                // Insert new user
                String insertQuery = "INSERT INTO users (username, password_hash) VALUES (?, ?)";
                pstmt = conn.prepareStatement(insertQuery);
                pstmt.setString(1, username.trim());
                pstmt.setString(2, hashedPassword);

                int affectedRows = pstmt.executeUpdate();

                if (affectedRows != 1) {
                    resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to register user.");
                } else {
                    // Generate session token or other success response
                    String sessionId = UUID.randomUUID().toString();
                    resp.setContentType("application/json");
                    resp.getWriter().write("{\"status\": \"success\", \"message\": \"Registration successful!\", \"session_id\": \"" + sessionId + "\"}");
                }

            } catch (SQLException e) {
                // Log exception details securely - never expose stack traces to users
                // Use a secure logging framework in production
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error.");
            } finally {
                // Close resources safely
                if (pstmt != null) pstmt.close();
                if (conn != null) conn.close();
            }
        } catch (NoSuchAlgorithmException e) {
            // Handle algorithm not found - should never happen with SHA-256
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal error.");
        } catch (IOException e) {
            // Handle I/O errors securely
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal error.");
        } catch (Exception e) {
            // Handle other exceptions securely
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal error.");
        }
    }
}