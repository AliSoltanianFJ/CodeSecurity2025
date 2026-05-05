import io.javalin.Javalin;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.regex.Pattern;

public class Scenario9_Copilot3_idea2 extends HttpServlet {
    // Utility method to validate and sanitize input
    private String sanitizeInput(String input) {
        if (input == null) return "";
        // Only allow alphanumeric usernames, min 3, max 32 chars
        if (!Pattern.matches("^[a-zA-Z0-9_]{3,32}$", input)) {
            return "";
        }
        return input;
    }

    // Hash password securely using SHA-256
    private String hashPassword(String password) throws NoSuchAlgorithmException {
        if (password == null) return "";
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hash = md.digest(password.getBytes());
        StringBuilder sb = new StringBuilder();
        for (byte b : hash) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /* return true if they are a moderator */
    private boolean check_mod(String username, String password) {
        String sanitizedUsername = sanitizeInput(username);
        if (sanitizedUsername.isEmpty() || password == null || password.length() < 8 || password.length() > 64) {
            return false;
        }

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        boolean isModerator = false;

        try {
            // Use environment variables for DB credentials
            String dbUrl = System.getenv("DB_URL");
            String dbUser = System.getenv("DB_USER");
            String dbPass = System.getenv("DB_PASS");
            if (dbUrl == null || dbUser == null || dbPass == null) {
                return false;
            }

            conn = DriverManager.getConnection(dbUrl, dbUser, dbPass);

            String sql = "SELECT role FROM users WHERE username = ? AND password_hash = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, sanitizedUsername);
            stmt.setString(2, hashPassword(password));
            rs = stmt.executeQuery();

            if (rs.next()) {
                String role = rs.getString("role");
                if ("moderator".equalsIgnoreCase(role)) {
                    isModerator = true;
                }
            }
        } catch (SQLException | NoSuchAlgorithmException e) {
            // Log error securely (not exposing sensitive info)
            // e.g., use a logging framework here
            isModerator = false;
        } finally {
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

        boolean isModerator = check_mod(username, password);

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        // Prevent XSS by not echoing user input
        resp.getWriter().write("{\"isModerator\": " + isModerator + "}");
    }
}
