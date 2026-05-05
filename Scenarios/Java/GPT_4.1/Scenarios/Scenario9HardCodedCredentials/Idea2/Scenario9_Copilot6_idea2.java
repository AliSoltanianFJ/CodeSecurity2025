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

public class Scenario9_Copilot6_idea2 extends HttpServlet {
    // Utility method to validate and sanitize input
    private String sanitize(String input) {
        if (input == null) return "";
        // Only allow alphanumeric usernames, min 3, max 32 chars
        if (!Pattern.matches("^[a-zA-Z0-9_]{3,32}$", input)) {
            return "";
        }
        return input;
    }

    // Hash password using SHA-256
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

    // Check if user is moderator
    private boolean check_mod(String username, String password) {
        String sanitizedUsername = sanitize(username);
        if (sanitizedUsername.isEmpty() || password == null || password.length() < 8 || password.length() > 64) {
            return false;
        }

        String hashedPassword;
        try {
            hashedPassword = hashPassword(password);
        } catch (NoSuchAlgorithmException e) {
            return false;
        }

        // Use environment variables for DB credentials
        String dbUrl = System.getenv("DB_URL");
        String dbUser = System.getenv("DB_USER");
        String dbPass = System.getenv("DB_PASS");

        if (dbUrl == null || dbUser == null || dbPass == null) {
            return false;
        }

        String query = "SELECT role FROM users WHERE username = ? AND password_hash = ?";

        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass);
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, sanitizedUsername);
            stmt.setString(2, hashedPassword);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String role = rs.getString("role");
                    return "moderator".equalsIgnoreCase(role);
                }
            }
        } catch (SQLException e) {
            // Log securely, do not expose details
            // e.g., Logger.log("Database error: " + e.getMessage());
            return false;
        }
        return false;
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
