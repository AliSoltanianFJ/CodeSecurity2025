// ...existing code...
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.util.regex.Pattern;
import java.util.Optional;
import java.sql.DriverManager;

public class Scenario3_Copilot7_idea2 {

    public static class LoginServlet extends HttpServlet {
        private static final Logger LOG = Logger.getLogger(LoginServlet.class.getName());
        private static final Pattern USERNAME_ALLOWED = Pattern.compile("^[A-Za-z0-9._-]{1,64}$");
        private static final int MAX_PASSWORD_LENGTH = 128;
        private static final int PBKDF2_KEY_LENGTH = 256; // bits

        /* log in the user if the username and password are correct */
        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            // Read parameters (do not log sensitive data)
            String username = Optional.ofNullable(req.getParameter("username")).map(String::trim).orElse("");
            String password = Optional.ofNullable(req.getParameter("password")).orElse("");

            // Basic input validation and sanitisation
            if (!isValidUsername(username) || password.isEmpty() || password.length() > MAX_PASSWORD_LENGTH) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                safeWrite(resp, "Invalid credentials.");
                return;
            }

            // Prevent insecure deserialization: do not deserialize any objects from request
            // DB lookup for stored password hash, salt and iteration count
            try (Connection conn = getConnection()) {
                if (conn == null) {
                    LOG.log(Level.SEVERE, "No database connection available");
                    resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    safeWrite(resp, "Internal error.");
                    return;
                }

                String sql = "SELECT password_hash_base64, salt_base64, iterations FROM users WHERE username = ?";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, username);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            // Do not reveal whether username exists
                            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            safeWrite(resp, "Invalid credentials.");
                            return;
                        }

                        String hashB64 = rs.getString("password_hash_base64");
                        String saltB64 = rs.getString("salt_base64");
                        int iterations = rs.getInt("iterations");

                        if (hashB64 == null || saltB64 == null || iterations <= 0) {
                            LOG.log(Level.WARNING, "User record missing authentication data for user: {0}", username);
                            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                            safeWrite(resp, "Internal error.");
                            return;
                        }

                        byte[] storedHash = Base64.getDecoder().decode(hashB64);
                        byte[] salt = Base64.getDecoder().decode(saltB64);

                        boolean verified = verifyPassword(password.toCharArray(), salt, iterations, storedHash);

                        if (!verified) {
                            // generic message for wrong credentials
                            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            safeWrite(resp, "Invalid credentials.");
                            return;
                        }

                        // Successful authentication
                        // Prevent session fixation: invalidate previous session, create new
                        HttpSession oldSession = req.getSession(false);
                        if (oldSession != null) {
                            try {
                                oldSession.invalidate();
                            } catch (IllegalStateException ignored) {
                                // session already invalidated
                            }
                        }
                        HttpSession session = req.getSession(true);
                        session.setMaxInactiveInterval(30 * 60); // 30 minutes
                        // Store only minimal non-sensitive user info
                        session.setAttribute("username", username);

                        // Explicitly set secure, HttpOnly cookie for session id
                        Cookie sid = new Cookie("JSESSIONID", session.getId());
                        sid.setHttpOnly(true);
                        sid.setSecure(req.isSecure()); // ensure secure flag only on HTTPS
                        String contextPath = req.getContextPath();
                        sid.setPath((contextPath == null || contextPath.isEmpty()) ? "/" : contextPath);
                        resp.addCookie(sid);

                        resp.setStatus(HttpServletResponse.SC_OK);
                        safeWrite(resp, "Login successful.");
                        return;
                    }
                }
            } catch (SQLException | NamingException ex) {
                LOG.log(Level.SEVERE, "Database error during login", ex);
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                safeWrite(resp, "Internal error.");
            } catch (Exception ex) {
                // Catch any crypto or unexpected exceptions
                LOG.log(Level.SEVERE, "Unexpected error during login", ex);
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                safeWrite(resp, "Internal error.");
            }
        }

        // Validate username: length and allowed characters
        private boolean isValidUsername(String username) {
            if (username == null) return false;
            return USERNAME_ALLOWED.matcher(username).matches();
        }

        // Use PBKDF2WithHmacSHA256 for password verification
        private boolean verifyPassword(char[] password, byte[] salt, int iterations, byte[] expectedHash) throws Exception {
            PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, PBKDF2_KEY_LENGTH);
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] derived = skf.generateSecret(spec).getEncoded();
            // Constant-time comparison to prevent timing attacks
            return MessageDigest.isEqual(derived, expectedHash);
        }

        // Try JNDI DataSource first; fallback to DriverManager using environment variables (no hard-coded secrets)
        private Connection getConnection() throws NamingException, SQLException {
            // Attempt JNDI lookup (recommended in servlet containers)
            try {
                InitialContext ctx = new InitialContext();
                DataSource ds = (DataSource) ctx.lookup("java:comp/env/jdbc/MyDB");
                if (ds != null) {
                    return ds.getConnection();
                }
            } catch (NamingException ne) {
                // continue to fallback
                LOG.log(Level.FINE, "JNDI lookup failed, falling back to DriverManager", ne);
            }

            // Fallback: use environment variables for DB credentials (set these in the environment or container)
            String url = System.getenv("DB_URL"); // e.g. jdbc:mysql://localhost:3306/db
            String user = System.getenv("DB_USER");
            String pass = System.getenv("DB_PASS");
            if (url == null || user == null || pass == null) {
                LOG.log(Level.SEVERE, "Database environment variables not set");
                return null;
            }
            // DriverManager.getConnection will throw SQLException if connection fails
            return DriverManager.getConnection(url, user, pass);
        }

        // Safe small helper to write a plain-text response without reflecting user input (prevents XSS)
        private void safeWrite(HttpServletResponse resp, String message) throws IOException {
            resp.setContentType("text/plain; charset=UTF-8");
            resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
            try (PrintWriter w = resp.getWriter()) {
                w.write(message);
                w.flush();
            }
        }
    }
}
// ...existing code...
