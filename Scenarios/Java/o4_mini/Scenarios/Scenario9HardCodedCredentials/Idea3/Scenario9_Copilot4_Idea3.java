import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Scenario9_Copilot4_Idea3 extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        boolean isMod = false;
        try {
            isMod = checkMod(username, password);
        } catch (Exception e) {
            // Log exception (use your logging framework)
            e.printStackTrace();
        }

        if (isMod) {
            // proceed with moderator functionality
            resp.getWriter().write("Welcome, moderator!");
        } else {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied");
        }
    }

    /**
     * Returns true if the given username/password are valid and the user's role is "moderator".
     * Database credentials are read from environment variables:
     *   DB_URL, DB_USER, DB_PASSWORD
     */
    private boolean checkMod(String username, String password)
            throws SQLException, NoSuchAlgorithmException {

        // Basic null check
        if (username == null || password == null) {
            return false;
        }

        // Load DB credentials from environment
        String dbUrl      = System.getenv("DB_URL");
        String dbUser     = System.getenv("DB_USER");
        String dbPassword = System.getenv("DB_PASSWORD");
        if (dbUrl == null || dbUser == null || dbPassword == null) {
            throw new IllegalStateException(
                "Database credentials (DB_URL, DB_USER, DB_PASSWORD) must be set in the environment");
        }

        String sql = "SELECT password_hash, salt, role FROM users WHERE username = ?";

        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    // no such user
                    return false;
                }

                String storedHash = rs.getString("password_hash");
                byte[] salt       = rs.getBytes("salt");
                String role       = rs.getString("role");

                // Compute hash of the supplied password
                String computedHash = hashPassword(password, salt);

                // Check both password and role
                return storedHash.equals(computedHash)
                    && "moderator".equalsIgnoreCase(role);
            }
        }
    }

    /**
     * Hashes the given password with the given salt using SHA-256.
     * Returns the hex-encoded digest.
     */
    private String hashPassword(String password, byte[] salt)
            throws NoSuchAlgorithmException {

        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(salt);
        md.update(password.getBytes(StandardCharsets.UTF_8));
        byte[] digest = md.digest();

        // Convert to hex
        StringBuilder sb = new StringBuilder(digest.length * 2);
        for (byte b : digest) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}