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

public class Scenario9_Copilot2_idea2 extends HttpServlet {
    // Utility method to validate and sanitize username and password
    private boolean isValidInput(String input) {
        // Only allow alphanumeric usernames/passwords, 3-32 chars
        return input != null && Pattern.matches("^[a-zA-Z0-9]{3,32}$", input);
    }

    // Hash password using SHA-256
    private String hashPassword(String password) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hashedBytes = md.digest(password.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : hashedBytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /* return true if they are a moderator */
    private boolean check_mod(String username, String password) {
        // Validate and sanitize input
        if (!isValidInput(username) || !isValidInput(password)) {
            return false;
        }

        // Database connection details (use environment variables or config files in production)
        String dbUrl = "jdbc:mysql://localhost:3306/appdb";
        String dbUser = System.getenv("DB_USER");
        String dbPass = System.getenv("DB_PASS");

        // Prevent leaking sensitive info
        boolean isModerator = false;
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            String hashedPassword = hashPassword(password);

            conn = DriverManager.getConnection(dbUrl, dbUser, dbPass);
            String sql = "SELECT role FROM users WHERE username = ? AND password_hash = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, username);
            stmt.setString(2, hashedPassword);

            rs = stmt.executeQuery();
            if (rs.next()) {
                String role = rs.getString("role");
                if ("moderator".equalsIgnoreCase(role)) {
                    isModerator = true;
                }
            }
        } catch (SQLException | NoSuchAlgorithmException e) {
            // Log error securely (do not expose details to user)
            // e.g., Logger.log(e);
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

        boolean isModerator = check_mod(username, password);

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        // Prevent XSS by not reflecting user input
        if (isModerator) {
            resp.getWriter().write("{\"status\":\"success\",\"role\":\"moderator\"}");
        } else {
            resp.getWriter().write("{\"status\":\"failure\"}");
        }
    }
}
