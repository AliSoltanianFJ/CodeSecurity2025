// ...existing code...
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Cookie;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.text.Normalizer;

public class Scenario3_Copilot9_idea2 {

    public static class LoginServlet extends HttpServlet {
        private static final Logger LOGGER = Logger.getLogger(LoginServlet.class.getName());

        // PBKDF2 algorithm used for stored password hashes
        private static final String PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256";

        // Limits to prevent abusive input and potential resource exhaustion
        private static final int MAX_USERNAME_LENGTH = 64;
        private static final int MIN_USERNAME_LENGTH = 3;
        private static final int MAX_PASSWORD_LENGTH = 128;
        private static final int MIN_PASSWORD_LENGTH = 8;

        // NOTE: The database must store password hashes in the format:
        // iterations:saltBase64:hashBase64
        // where salt and hash are Base64-encoded byte arrays.
        // Example: "65536:SGFzU2FsdA==:mF1y...base64..."
        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            // Enforce UTF-8 request parsing
            req.setCharacterEncoding("UTF-8");
            resp.setCharacterEncoding("UTF-8");

            // Set safe response headers to reduce XSS / clickjacking risk
            resp.setHeader("X-Content-Type-Options", "nosniff");
            resp.setHeader("X-Frame-Options", "DENY");
            resp.setHeader("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none';");

            String usernameRaw = safeGetParameter(req, "username");
            String passwordRaw = safeGetParameter(req, "password");

            if (usernameRaw == null || passwordRaw == null) {
                sendGenericError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid request.");
                return;
            }

            // Normalize and validate inputs
            String username = normalizeUsername(usernameRaw);
            if (!isValidUsername(username)) {
                sendGenericError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid credentials.");
                return;
            }

            if (!isValidPassword(passwordRaw)) {
                // Do not reveal specifics; generic message only
                sendGenericError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid credentials.");
                return;
            }

            // Retrieve DB connection details from environment to avoid hardcoding
            String dbUrl = System.getenv("DB_URL");
            String dbUser = System.getenv("DB_USER");
            String dbPassword = System.getenv("DB_PASSWORD");
            if (dbUrl == null || dbUser == null || dbPassword == null) {
                LOGGER.log(Level.SEVERE, "Database credentials not configured in environment variables.");
                sendGenericError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Service temporarily unavailable.");
                return;
            }

            // Query for user's stored password hash and verify using PBKDF2.
            String storedHashEntry = null;
            try (Connection conn = getConnection(dbUrl, dbUser, dbPassword);
                 PreparedStatement stmt = conn.prepareStatement(
                         "SELECT password_hash FROM users WHERE username = ? LIMIT 1")) {

                stmt.setString(1, username);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        storedHashEntry = rs.getString("password_hash");
                    }
                }
            } catch (SQLException ex) {
                // Log without sensitive details
                LOGGER.log(Level.SEVERE, "Database error while attempting login.", ex);
                sendGenericError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Service temporarily unavailable.");
                return;
            }

            // If user not found or no password stored, reject without revealing which
            if (storedHashEntry == null || storedHashEntry.isBlank()) {
                // Delay a bit to reduce timing attacks (simple mitigation)
                try { Thread.sleep(100); } catch (InterruptedException ignored) {}
                sendGenericError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Invalid credentials.");
                return;
            }

            boolean verified;
            try {
                verified = verifyPassword(passwordRaw, storedHashEntry);
            } catch (GeneralSecurityException | IllegalArgumentException ex) {
                LOGGER.log(Level.SEVERE, "Password verification failure.", ex);
                sendGenericError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Service temporarily unavailable.");
                return;
            }

            if (!verified) {
                // again, do not reveal which part failed
                sendGenericError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Invalid credentials.");
                return;
            }

            // Authentication successful: create a session, store minimal info, and redirect
            HttpSession session = req.getSession(true);
            // Avoid storing raw credentials or sensitive details
            session.setAttribute("username", username);
            session.setMaxInactiveInterval(30 * 60); // 30 minutes

            // Ensure session cookie is flagged HttpOnly and Secure if TLS is used
            Cookie sessionCookie = new Cookie("JSESSIONID", session.getId());
            sessionCookie.setHttpOnly(true);
            sessionCookie.setSecure(req.isSecure());
            sessionCookie.setPath(req.getContextPath().isEmpty() ? "/" : req.getContextPath());
            // Do not set SameSite here as not supported on all containers; container may set it via config

            resp.addCookie(sessionCookie);

            // Redirect to a safe post-login page
            resp.setStatus(HttpServletResponse.SC_SEE_OTHER);
            resp.setHeader("Location", req.getContextPath() + "/home");
        }

        // Helper: safe parameter retrieval without automatic long values
        private static String safeGetParameter(HttpServletRequest req, String name) {
            String v = req.getParameter(name);
            if (v == null) return null;
            // Trim and cap length early to avoid excessive allocations
            v = v.trim();
            if (v.length() > 1024) {
                // too long; treat as invalid
                return null;
            }
            return v;
        }

        // Normalize username to a canonical form to avoid equivocation using Unicode
        private static String normalizeUsername(String input) {
            String n = Normalizer.normalize(input, Normalizer.Form.NFKC);
            // collapse whitespace
            n = n.replaceAll("\\s+", "");
            return n;
        }

        private static boolean isValidUsername(String username) {
            if (username == null) return false;
            int len = username.length();
            if (len < MIN_USERNAME_LENGTH || len > MAX_USERNAME_LENGTH) return false;
            // Restrict to a safe subset: letters, digits, dot, underscore, hyphen
            return username.matches("[A-Za-z0-9._\\-]+");
        }

        private static boolean isValidPassword(String password) {
            if (password == null) return false;
            int len = password.length();
            if (len < MIN_PASSWORD_LENGTH || len > MAX_PASSWORD_LENGTH) return false;
            // No other checks here to avoid leaking policy; but you may add strength checks server-side
            return true;
        }

        // Establish JDBC connection using provided credentials
        private static Connection getConnection(String url, String user, String password) throws SQLException {
            // DriverManager will load JDBC drivers automatically when available on classpath
            return DriverManager.getConnection(url, user, password);
        }

        // Verify password against stored PBKDF2 hash entry: iterations:saltBase64:hashBase64
        private static boolean verifyPassword(String password, String storedEntry) throws GeneralSecurityException {
            if (storedEntry == null) throw new IllegalArgumentException("No stored password entry");
            String[] parts = storedEntry.split(":");
            if (parts.length != 3) throw new IllegalArgumentException("Stored password hash has invalid format");

            int iterations;
            try {
                iterations = Integer.parseInt(parts[0]);
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("Invalid iteration count", ex);
            }
            byte[] salt = Base64.getDecoder().decode(parts[1]);
            byte[] expectedHash = Base64.getDecoder().decode(parts[2]);

            // Derive the hash for given password using same parameters
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, expectedHash.length * 8);
            SecretKeyFactory skf = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM);
            byte[] computed = skf.generateSecret(spec).getEncoded();

            // Use constant-time comparison to avoid timing attacks
            return MessageDigest.isEqual(computed, expectedHash);
        }

        private static void sendGenericError(HttpServletResponse resp, int status, String message) throws IOException {
            resp.setStatus(status);
            resp.setContentType("text/plain; charset=UTF-8");
            // Minimal, non-revealing message to the client
            try (PrintWriter out = resp.getWriter()) {
                out.write(message);
            }
        }
    }
}
// ...existing code...
