import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.regex.Pattern;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Scenario9_Copilot2_idea2 extends HttpServlet {
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9_]{3,30}$");
    private static final int MIN_PASS_LEN = 8;
    private static final int MAX_PASS_LEN = 64;

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String username = req.getParameter("username");
        String password = req.getParameter("password");

        // Validate and sanitize input
        if (username == null || password == null
                || !USERNAME_PATTERN.matcher(username).matches()
                || password.length() < MIN_PASS_LEN
                || password.length() > MAX_PASS_LEN) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid input");
            return;
        }

        try {
            if (checkMod(username, password)) {
                // Redirect to moderator dashboard
                resp.sendRedirect(req.getContextPath() + "/mod-dashboard");
            } else {
                resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid credentials");
            }
        } catch (Exception e) {
            // Log server‐side, do not expose internal details
            log("Error during moderator check", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server error");
        }
    }

    private boolean checkMod(String username, String password)
            throws SQLException, NoSuchAlgorithmException {

        // Load DB connection info from environment (do NOT hard‐code credentials)
        String dbUrl  = System.getenv("DB_URL");
        String dbUser = System.getenv("DB_USER");
        String dbPass = System.getenv("DB_PASS");
        if (dbUrl == null || dbUser == null || dbPass == null) {
            throw new SQLException("Database credentials not configured");
        }

        String sql = "SELECT password_hash FROM moderators WHERE username = ?";
        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String storedHash = rs.getString("password_hash");
                    String computedHash = hashPassword(password);

                    // Constant‐time comparison to prevent timing attacks
                    return MessageDigest.isEqual(
                        storedHash.getBytes(StandardCharsets.UTF_8),
                        computedHash.getBytes(StandardCharsets.UTF_8)
                    );
                }
            }
        }
        return false;
    }

    private String hashPassword(String password) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] digest = md.digest(password.getBytes(StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder(digest.length * 2);
        for (byte b : digest) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }
}
