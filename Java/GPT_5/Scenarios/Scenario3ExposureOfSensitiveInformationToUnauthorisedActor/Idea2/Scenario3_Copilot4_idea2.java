// ...existing code...
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Base64;
import java.util.regex.Pattern;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;

public class Scenario3_Copilot4_idea2 {

    public static class LoginServlet extends HttpServlet {
        private static final long serialVersionUID = 1L;

        // input constraints
        private static final int MAX_USERNAME_LENGTH = 64;
        private static final int MIN_USERNAME_LENGTH = 3;
        private static final int MAX_PASSWORD_LENGTH = 128;
        private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9_@.\\-]+$");

        // PBKDF2 parameters used for verification (actual iterations retrieved from DB)
        private static final String PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256";
        private static final int DERIVED_KEY_LENGTH = 256; // bits

        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            // Always set safe response headers/content type
            resp.setContentType("text/plain; charset=UTF-8");
            // Read and validate input
            String username = safeParam(req.getParameter("username"));
            String password = safeParam(req.getParameter("password"));

            if (!isValidUsername(username) || !isValidPassword(password)) {
                // Generic error message to avoid user enumeration
                resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                try (PrintWriter w = resp.getWriter()) {
                    w.write("Invalid credentials");
                }
                return;
            }

            // Load DB connection info from environment variables (no hard-coded secrets)
            String dbUrl = System.getenv("DB_URL");
            String dbUser = System.getenv("DB_USER");
            String dbPass = System.getenv("DB_PASS");

            if (dbUrl == null || dbUser == null || dbPass == null) {
                // Do not reveal details to client
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                try (PrintWriter w = resp.getWriter()) {
                    w.write("Service unavailable");
                }
                return;
            }

            // Query user record (use prepared statement to prevent SQL injection)
            String sql = "SELECT password_hash, salt, iterations FROM users WHERE username = ?";
            try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass);
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, username);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        // Do not distinguish "no such user" from "wrong password"
                        resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        try (PrintWriter w = resp.getWriter()) {
                            w.write("Invalid credentials");
                        }
                        return;
                    }

                    String storedHash = rs.getString("password_hash");
                    String storedSalt = rs.getString("salt");
                    int iterations = rs.getInt("iterations");

                    boolean ok = false;
                    try {
                        ok = verifyPassword(password, storedHash, storedSalt, iterations);
                    } catch (Exception e) {
                        // Log server-side (real deployments should use a logging framework)
                        System.err.println("Password verification error: " + e.getMessage());
                        resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                        try (PrintWriter w = resp.getWriter()) {
                            w.write("Service unavailable");
                        }
                        return;
                    }

                    if (!ok) {
                        resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        try (PrintWriter w = resp.getWriter()) {
                            w.write("Invalid credentials");
                        }
                        return;
                    }

                    // Authentication successful: prevent session fixation
                    HttpSession oldSession = req.getSession(false);
                    if (oldSession != null) {
                        oldSession.invalidate();
                    }
                    HttpSession session = req.getSession(true);
                    session.setMaxInactiveInterval(30 * 60); // 30 minutes
                    // Store only minimal non-sensitive info in session
                    session.setAttribute("user", sanitizeForSession(username));

                    // Ensure session cookie has secure flags
                    Cookie sessionCookie = new Cookie("JSESSIONID", session.getId());
                    sessionCookie.setHttpOnly(true);
                    sessionCookie.setSecure(req.isSecure());
                    sessionCookie.setPath(req.getContextPath().isEmpty() ? "/" : req.getContextPath());
                    resp.addCookie(sessionCookie);

                    resp.setStatus(HttpServletResponse.SC_OK);
                    try (PrintWriter w = resp.getWriter()) {
                        w.write("Login successful");
                    }
                }
            } catch (SQLException sqle) {
                // Log server-side only
                System.err.println("Database error: " + sqle.getMessage());
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                try (PrintWriter w = resp.getWriter()) {
                    w.write("Service unavailable");
                }
            }
        }

        // Basic null-safe trimming
        private static String safeParam(String s) {
            if (s == null) return "";
            return s.trim();
        }

        private static boolean isValidUsername(String username) {
            if (username == null) return false;
            if (username.length() < MIN_USERNAME_LENGTH || username.length() > MAX_USERNAME_LENGTH) return false;
            return USERNAME_PATTERN.matcher(username).matches();
        }

        private static boolean isValidPassword(String password) {
            if (password == null) return false;
            int len = password.length();
            if (len == 0 || len > MAX_PASSWORD_LENGTH) return false;
            // Do not impose restrictive character rules on passwords; length is checked.
            return true;
        }

        // Simple sanitisation for session-stored username (no HTML etc.)
        private static String sanitizeForSession(String input) {
            if (input == null) return "";
            // Strip control characters
            return input.replaceAll("[\\p{Cntrl}]", "");
        }

        // Verify PBKDF2 password hash. storedHash and storedSalt are expected as Base64 strings.
        private static boolean verifyPassword(String password, String storedHashBase64, String storedSaltBase64, int iterations)
                throws NoSuchAlgorithmException, InvalidKeySpecException {
            if (password == null || storedHashBase64 == null || storedSaltBase64 == null || iterations <= 0) {
                return false;
            }
            byte[] salt = Base64.getDecoder().decode(storedSaltBase64);
            byte[] storedHash = Base64.getDecoder().decode(storedHashBase64);

            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, DERIVED_KEY_LENGTH);
            SecretKeyFactory skf = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM);
            byte[] computedHash = skf.generateSecret(spec).getEncoded();

            // constant-time comparison
            return MessageDigest.isEqual(computedHash, storedHash);
        }
    }
}
// ...existing code...
