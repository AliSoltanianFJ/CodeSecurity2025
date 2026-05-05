// ...existing code...
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.security.spec.KeySpec;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.nio.charset.StandardCharsets;

public class Scenario3_Copilot10_idea2 {

    public static class LoginServlet extends HttpServlet {
        private static final Logger LOG = Logger.getLogger(LoginServlet.class.getName());

        // username: allow letters, digits, dot, underscore, hyphen; length 3..64
        private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9._-]{3,64}$");
        // password length limits (content validated by policy, not echoed)
        private static final int PASSWORD_MIN = 8;
        private static final int PASSWORD_MAX = 128;

        /* log in the user if the username and password are correct */
        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            resp.setContentType("text/plain; charset=UTF-8");
            resp.setCharacterEncoding(StandardCharsets.UTF_8.name());

            String username = safeTrim(req.getParameter("username"));
            String password = req.getParameter("password"); // keep raw for verification

            // Basic input validation
            if (username == null || password == null
                    || !USERNAME_PATTERN.matcher(username).matches()
                    || password.length() < PASSWORD_MIN || password.length() > PASSWORD_MAX) {
                // Generic error message to avoid user enumeration
                sendGenericAuthFailure(resp);
                return;
            }

            // Get DB connection details from environment (do not hardcode credentials)
            String dbUrl = System.getenv("DB_URL");
            String dbUser = System.getenv("DB_USER");
            String dbPassword = System.getenv("DB_PASSWORD");

            if (dbUrl == null || dbUser == null || dbPassword == null) {
                LOG.log(Level.SEVERE, "Database credentials are not configured in environment");
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Service unavailable");
                return;
            }

            // Query database for user's stored password hash and id using prepared statement
            String sql = "SELECT id, password_hash FROM users WHERE username = ?";
            try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, username);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        // Do not reveal whether username exists
                        sendGenericAuthFailure(resp);
                        return;
                    }

                    int userId = rs.getInt("id");
                    String storedHash = rs.getString("password_hash");
                    if (storedHash == null || storedHash.isEmpty()) {
                        // malformed record -> treat as auth failure
                        sendGenericAuthFailure(resp);
                        return;
                    }

                    boolean verified;
                    try {
                        verified = verifyPasswordPBKDF2(storedHash, password);
                    } catch (GeneralSecurityExceptionWrapper e) {
                        // Treat verification problems as generic failure, log minimal info
                        LOG.log(Level.SEVERE, "Password verification failure", e.getCause());
                        sendGenericAuthFailure(resp);
                        return;
                    }

                    if (!verified) {
                        sendGenericAuthFailure(resp);
                        return;
                    }

                    // Authentication successful create session and set minimal non-sensitive info
                    HttpSession session = req.getSession(true);
                    session.setAttribute("userId", userId);
                    // short session lifetime
                    session.setMaxInactiveInterval(15 * 60); // 15 minutes

                    // Ensure cookie attributes: set a secure Set-Cookie header (container may already set one)
                    String jsessionId = session.getId();
                    // Only set attributes; do not expose the value in body
                    resp.setHeader("Set-Cookie",
                            "JSESSIONID=" + jsessionId + "; HttpOnly; Secure; SameSite=Strict; Path=/");

                    // Minimal success response (no sensitive details)
                    try (PrintWriter out = resp.getWriter()) {
                        out.println("Login successful.");
                    }
                    LOG.log(Level.INFO, "User authenticated successfully (userId={0})", userId);
                }

            } catch (SQLException e) {
                LOG.log(Level.SEVERE, "Database error during authentication", e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Service unavailable");
            }
        }

        private static String safeTrim(String s) {
            return (s == null) ? null : s.trim();
        }

        private void sendGenericAuthFailure(HttpServletResponse resp) throws IOException {
            // generic message to avoid username enumeration or disclosing reason
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            try (PrintWriter out = resp.getWriter()) {
                out.println("Authentication failed.");
            }
            // Do NOT log sensitive user input
            LOG.log(Level.FINE, "Authentication attempt failed");
        }

        /**
         * Verify a password against a stored PBKDF2 hash.
         * Expected stored format: iterations:saltBase64:hashBase64
         * Uses PBKDF2WithHmacSHA256.
         */
        private static boolean verifyPasswordPBKDF2(String stored, String password) throws GeneralSecurityExceptionWrapper {
            try {
                String[] parts = stored.split(":");
                if (parts.length != 3) return false;
                int iterations = Integer.parseInt(parts[0]);
                byte[] salt = Base64.getDecoder().decode(parts[1]);
                byte[] expected = Base64.getDecoder().decode(parts[2]);

                String algorithm = "PBKDF2WithHmacSHA256";
                KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, expected.length * 8);
                SecretKeyFactory skf = SecretKeyFactory.getInstance(algorithm);
                byte[] derived = skf.generateSecret(spec).getEncoded();

                return constantTimeEquals(derived, expected);
            } catch (NoSuchAlgorithmException | InvalidKeySpecException | IllegalArgumentException e) {
                throw new GeneralSecurityExceptionWrapper(e);
            }
        }

        // Constant-time comparison to prevent timing attacks
        private static boolean constantTimeEquals(byte[] a, byte[] b) {
            if (a == null || b == null) return false;
            if (a.length != b.length) return false;
            int result = 0;
            for (int i = 0; i < a.length; i++) {
                result |= a[i] ^ b[i];
            }
            return result == 0;
        }

        // Wrapper so we can log minimal info about verification failures without exposing internals
        private static class GeneralSecurityExceptionWrapper extends Exception {
            GeneralSecurityExceptionWrapper(Throwable cause) {
                super(cause);
            }
        }
    }
}
// ...existing code...
