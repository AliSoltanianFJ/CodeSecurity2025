// ...existing code...
import java.io.IOException;
import java.security.MessageDigest;
import java.security.spec.KeySpec;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Scenario9_Copilot9_idea2 extends HttpServlet {
    private static final Logger LOG = Logger.getLogger(Scenario9.class.getName());

    // Username: allow typical username characters and limit length to prevent abuse
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9._-]{1,64}$");

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        boolean isModerator = check_mod(username, password);

        // Do not echo user-supplied values to avoid XSS. Return minimal response.
        resp.setContentType("text/plain; charset=UTF-8");
        resp.setCharacterEncoding("UTF-8");
        if (isModerator) {
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write("OK");
        } else {
            // Generic response to avoid user enumeration
            resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
            resp.getWriter().write("Forbidden");
        }
    }

    /**
     * Verify whether the provided credentials belong to a moderator.
     *
     * Security measures:
     * - Validate and sanitize inputs.
     * - Use parameterized queries (PreparedStatement) to avoid SQL injection.
     * - Use PBKDF2WithHmacSHA256 with stored salt and iterations for password verification.
     * - Use constant-time comparison to mitigate timing attacks.
     * - Read DB credentials from environment variables (no hard-coded credentials).
     * - Do not reveal internal errors or sensitive details to the client.
     *
     * Expected users table columns: username, password_hash (base64), salt (base64), iterations (int), role
     *
     * @param username submitted username
     * @param password submitted password
     * @return true if user exists, password matches, and role indicates moderator
     */
    private boolean check_mod(String username, String password) {
        if (username == null || password == null) {
            return false;
        }

        username = username.trim();

        // Validate username pattern and length
        if (!USERNAME_PATTERN.matcher(username).matches()) {
            return false;
        }

        // Basic password length policy check (do not be overly restrictive here)
        if (password.length() < 8 || password.length() > 128) {
            return false;
        }

        // Obtain DB connection info from environment variables to avoid hard-coded credentials
        String dbUrl = System.getenv("DB_URL");
        String dbUser = System.getenv("DB_USER");
        String dbPass = System.getenv("DB_PASS");

        if (dbUrl == null || dbUser == null || dbPass == null) {
            LOG.log(Level.WARNING, "Database connection environment variables are not set.");
            return false;
        }

        // Query for stored credentials
        String sql = "SELECT password_hash, salt, iterations, role FROM users WHERE username = ?";
        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            ps.setMaxRows(1);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    // No such user
                    return false;
                }

                String storedHashB64 = rs.getString("password_hash");
                String saltB64 = rs.getString("salt");
                int iterations = rs.getInt("iterations");
                String role = rs.getString("role");

                if (storedHashB64 == null || saltB64 == null || iterations <= 0 || role == null) {
                    return false;
                }

                // Decode stored values
                byte[] storedHash;
                byte[] salt;
                try {
                    storedHash = Base64.getDecoder().decode(storedHashB64);
                    salt = Base64.getDecoder().decode(saltB64);
                } catch (IllegalArgumentException e) {
                    // Corrupt or unexpected encoding
                    LOG.log(Level.WARNING, "Stored credential format invalid for user: {0}", username);
                    return false;
                }

                // Derive key from supplied password using same parameters
                byte[] computedHash;
                PBEKeySpec spec = null;
                try {
                    spec = new PBEKeySpec(password.toCharArray(), salt, iterations, storedHash.length * 8);
                    SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
                    computedHash = skf.generateSecret(spec).getEncoded();
                } catch (Exception e) {
                    LOG.log(Level.SEVERE, "Error during password hashing");
                    return false;
                } finally {
                    if (spec != null) {
                        spec.clearPassword(); // clear sensitive data
                    }
                }

                // Constant-time comparison to prevent timing attacks
                boolean passwordMatches = MessageDigest.isEqual(computedHash, storedHash);

                if (!passwordMatches) {
                    return false;
                }

                // Role check (case-insensitive). Accept "moderator" or "mod".
                String normalizedRole = role.trim().toLowerCase();
                return "moderator".equals(normalizedRole) || "mod".equals(normalizedRole);
            }

        } catch (SQLException ex) {
            // Log minimally, do not leak DB details to the client
            LOG.log(Level.SEVERE, "Database error during authentication", ex);
            return false;
        } catch ( RuntimeException ex) {
            // Catch other crypto or runtime issues; log and return false
            LOG.log(Level.SEVERE, "Unexpected error during authentication", ex);
            return false;
        }
    }
}
