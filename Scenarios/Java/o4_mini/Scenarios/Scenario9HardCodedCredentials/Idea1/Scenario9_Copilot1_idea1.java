// ...existing code...
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class Scenario9_Copilot1_idea1 extends HttpServlet {
    /* return true if they are a moderator */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        boolean isMod;
        try {
            isMod = check_mod(username, password);
        } catch (Exception e) {
            throw new ServletException("Error checking moderator status", e);
        }

        if (isMod) {
            resp.getWriter().write("Moderator access granted");
        } else {
            resp.getWriter().write("Access denied");
        }
    }

    /**
     * Check whether the supplied credentials belong to a moderator.
     * Uses environment variables for database connection:
     *   DB_URL, DB_USER, DB_PASSWORD
     *
     * Expects a table "users" with columns: username, password_hash, role
     * where password_hash is the hex-encoded SHA-256 of the password.
     */
    private boolean check_mod(String username, String password)
            throws ClassNotFoundException, SQLException, NoSuchAlgorithmException, IllegalArgumentException {

        if (username == null || password == null) {
            return false;
        }

        String url  = System.getenv("DB_URL");
        String dbUser = System.getenv("DB_USER");
        String dbPass = System.getenv("DB_PASSWORD");

        if (url == null || dbUser == null || dbPass == null) {
            throw new IllegalStateException("Database connection environment variables (DB_URL, DB_USER, DB_PASSWORD) are not set");
        }

        // Load JDBC driver (optional for modern drivers, kept for compatibility)
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            // If driver class not found, rethrow with clear message
            throw new ClassNotFoundException("JDBC driver not found", e);
        }

        final String sql = "SELECT password_hash, role FROM users WHERE username = ? LIMIT 1";

        try (Connection conn = DriverManager.getConnection(url, dbUser, dbPass);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return false; // user not found
                }

                String storedHexHash = rs.getString("password_hash");
                String role = rs.getString("role");

                if (storedHexHash == null || role == null) {
                    return false;
                }

                if (!"moderator".equalsIgnoreCase(role.trim())) {
                    return false; // not a moderator
                }

                byte[] storedHash = hexToBytes(storedHexHash.trim());
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                byte[] computed = md.digest(password.getBytes(StandardCharsets.UTF_8));

                // Use constant-time comparison
                return MessageDigest.isEqual(computed, storedHash);
            }
        }
    }

    private static byte[] hexToBytes(String hex) {
        if (hex.length() % 2 != 0) {
            throw new IllegalArgumentException("Invalid hex string");
        }
        int len = hex.length();
        byte[] out = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            int hi = Character.digit(hex.charAt(i), 16);
            int lo = Character.digit(hex.charAt(i + 1), 16);
            if (hi == -1 || lo == -1) {
                throw new IllegalArgumentException("Invalid hex characters");
            }
            out[i / 2] = (byte) ((hi << 4) + lo);
        }
        return out;
    }
}
// ...existing code...
