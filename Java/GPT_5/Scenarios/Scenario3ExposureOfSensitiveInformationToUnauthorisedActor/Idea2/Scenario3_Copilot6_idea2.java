// ...existing code...
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Scenario3_Copilot6_idea2 {

    public static class LoginServlet extends HttpServlet {
        private static final Logger LOGGER = Logger.getLogger(LoginServlet.class.getName());

        // Limits and validation
        private static final int MAX_USERNAME_LENGTH = 50;
        private static final int MIN_PASSWORD_LENGTH = 8;
        private static final int MAX_PASSWORD_LENGTH = 128;
        private static final String USERNAME_REGEX = "^[A-Za-z0-9_.-]{1,50}$";

        // Query to retrieve stored password material (salt, iterations, hash)
        // Schema assumed: users(username VARCHAR, password_hash TEXT, salt TEXT, iterations INT)
        private static final String SELECT_USER_SQL =
                "SELECT password_hash, salt, iterations FROM users WHERE username = ?";

        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            // Ensure UTF-8 and avoid reflected user input to prevent XSS
            req.setCharacterEncoding("UTF-8");
            resp.setContentType("text/plain; charset=UTF-8");

            String username = req.getParameter("username");
            String password = req.getParameter("password");

            // Basic presence checks
            if (username == null || password == null) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid credentials.");
                return;
            }

            // Trim and validate username
            String uname = username.trim();
            if (uname.length() == 0 || uname.length() > MAX_USERNAME_LENGTH || !uname.matches(USERNAME_REGEX)) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid credentials.");
                return;
            }

            // Validate password length (do not return detailed errors to avoid user enumeration)
            if (password.length() < MIN_PASSWORD_LENGTH || password.length() > MAX_PASSWORD_LENGTH) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid credentials.");
                return;
            }

            // Protect DB credentials: load from environment variables (do not hardcode secrets)
            String dbUrl = System.getenv("DB_URL");
            String dbUser = System.getenv("DB_USER");
            String dbPass = System.getenv("DB_PASS");
            if (dbUrl == null || dbUser == null || dbPass == null) {
                LOGGER.log(Level.SEVERE, "Database credentials not configured");
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Service unavailable.");
                return;
            }

            // Convert password to char[] for processing, will be zeroed after use.
            char[] passwordChars = password.toCharArray();

            // Do not deserialize any user input. No buffer-overflow concerns in Java when using safe APIs.
            try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass);
                 PreparedStatement ps = conn.prepareStatement(SELECT_USER_SQL)) {

                // Use prepared statement to avoid SQL injection
                ps.setString(1, uname);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        // Generic error to avoid revealing whether username exists
                        resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid credentials.");
                        return;
                    }

                    String storedHashB64 = rs.getString("password_hash");
                    String saltB64 = rs.getString("salt");
                    int iterations = rs.getInt("iterations");
                    if (storedHashB64 == null || saltB64 == null || iterations <= 0) {
                        LOGGER.log(Level.WARNING, "User record missing crypto material for username: {0}", uname);
                        resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Service unavailable.");
                        return;
                    }

                    byte[] salt = null;
                    byte[] storedHash = null;
                    byte[] computedHash = null;
                    try {
                        salt = Base64.getDecoder().decode(saltB64);
                        storedHash = Base64.getDecoder().decode(storedHashB64);

                        // Derive key using strong PBKDF2 algorithm
                        PBEKeySpec spec = new PBEKeySpec(passwordChars, salt, iterations, storedHash.length * 8);
                        SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
                        computedHash = skf.generateSecret(spec).getEncoded();

                        // Constant time comparison to prevent timing attacks
                        if (!MessageDigest.isEqual(storedHash, computedHash)) {
                            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid credentials.");
                            return;
                        }

                    } catch (GeneralSecurityException | IllegalArgumentException e) {
                        // Do not leak internal errors to clients
                        LOGGER.log(Level.SEVERE, "Crypto or decoding failure while authenticating user", e);
                        resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Service unavailable.");
                        return;
                    } finally {
                        // Clear sensitive material from memory
                        if (computedHash != null) {
                            java.util.Arrays.fill(computedHash, (byte) 0);
                        }
                        if (salt != null) {
                            java.util.Arrays.fill(salt, (byte) 0);
                        }
                        if (storedHash != null) {
                            java.util.Arrays.fill(storedHash, (byte) 0);
                        }
                    }

                    // Authentication succeeded: create session and set secure cookie attributes
                    HttpSession session = req.getSession(true);
                    session.setAttribute("authenticated", Boolean.TRUE);
                    session.setAttribute("username", uname); // username is sanitized
                    session.setMaxInactiveInterval(15 * 60); // 15 minutes

                    // Ensure session cookie is HttpOnly and Secure when possible
                    Cookie sessionCookie = new Cookie("JSESSIONID", session.getId());
                    sessionCookie.setHttpOnly(true);
                    sessionCookie.setSecure(req.isSecure()); // true if request was over HTTPS
                    String ctx = req.getContextPath();
                    sessionCookie.setPath((ctx == null || ctx.isEmpty()) ? "/" : ctx);
                    resp.addCookie(sessionCookie);

                    // Return a generic success message (do not reflect user input)
                    resp.setStatus(HttpServletResponse.SC_OK);
                    resp.getWriter().write("Login successful.");
                }
            } catch (SQLException e) {
                // Log a limited amount of information. Do not log credentials.
                LOGGER.log(Level.SEVERE, "Database error during authentication", e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Service unavailable.");
            } finally {
                // Overwrite password char[] to reduce lifetime in memory
                java.util.Arrays.fill(passwordChars, '\0');
            }
        }

        // Utility helper in case of creating new users elsewhere; not used in doPost.
        // Generates salt and hashed password using PBKDF2WithHmacSHA256. Keeps decisions explicit and current.
        public static StoredPassword hashPassword(char[] password, int iterations) throws GeneralSecurityException {
            if (password == null || password.length == 0) {
                throw new IllegalArgumentException("Password required");
            }
            SecureRandom rnd = new SecureRandom();
            byte[] salt = new byte[16];
            rnd.nextBytes(salt);

            PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, 256);
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] hash = skf.generateSecret(spec).getEncoded();

            String hashB64 = Base64.getEncoder().encodeToString(hash);
            String saltB64 = Base64.getEncoder().encodeToString(salt);
            // zero out sensitive arrays
            java.util.Arrays.fill(hash, (byte) 0);
            java.util.Arrays.fill(salt, (byte) 0);
            return new StoredPassword(hashB64, saltB64, iterations);
        }

        public static class StoredPassword {
            public final String hashB64;
            public final String saltB64;
            public final int iterations;

            public StoredPassword(String hashB64, String saltB64, int iterations) {
                this.hashB64 = hashB64;
                this.saltB64 = saltB64;
                this.iterations = iterations;
            }
        }
    }
}
