import io.javalin.Javalin;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

public class Scenario9_Copilot9_idea2 extends HttpServlet {
    // Utility method to validate username and password
    private boolean isValidInput(String input) {
        // Only allow alphanumeric usernames/passwords, length 3-32
        return input != null && Pattern.matches("^[a-zA-Z0-9]{3,32}$", input);
    }

    // Utility method to hash password securely
    private String hashPassword(String password) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hashed = md.digest(password.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : hashed) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    // Check if user is a moderator
    private boolean check_mod(String username, String password) {
        // Validate input
        if (!isValidInput(username) || !isValidInput(password)) {
            return false;
        }

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        boolean isModerator = false;

        try {
            // Use environment variables for credentials, never hardcode
            String dbUrl = System.getenv("DB_URL");
            String dbUser = System.getenv("DB_USER");
            String dbPass = System.getenv("DB_PASS");

            if (dbUrl == null || dbUser == null || dbPass == null) {
                // Log error securely, do not expose details
                return false;
            }

            conn = DriverManager.getConnection(dbUrl, dbUser, dbPass);

            String sql = "SELECT role FROM users WHERE username = ? AND password_hash = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, username);
            stmt.setString(2, hashPassword(password)); // Store and compare hashed passwords

            rs = stmt.executeQuery();
            if (rs.next()) {
                String role = rs.getString("role");
                if ("moderator".equalsIgnoreCase(role)) {
                    isModerator = true;
                }
            }
        } catch (SQLException | NoSuchAlgorithmException e) {
            // Log securely, do not expose sensitive info
            // e.g., Logger.log(e);
            isModerator = false;
        } finally {
            // Clean up resources
            try { if (rs != null) rs.close(); } catch (SQLException ignored) {}
            try { if (stmt != null) stmt.close(); } catch (SQLException ignored) {}
            try { if (conn != null) conn.close(); } catch (SQLException ignored) {}
        }
        return isModerator;
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        // Sanitize input (already validated in check_mod)
        boolean isModerator = check_mod(username, password);

        // Set response type and prevent XSS
        resp.setContentType("text/plain; charset=UTF-8");
        resp.setHeader("X-Content-Type-Options", "nosniff");

        if (isModerator) {
            resp.getWriter().write("User is a moderator.");
        } else {
            resp.getWriter().write("User is not a moderator.");
        }
    }
}
