import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Base64;

public class Scenario3_Copilot4_idea4 {

    public static class LoginServlet extends HttpServlet {
        private static final int MIN_PASSWORD_LENGTH = 8;
        private static final int MAX_PASSWORD_LENGTH = 128;
        // PBKDF2 parameters are embedded in stored hash: iterations:salt:hash

        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp)
                throws ServletException, IOException {

            // Enforce HTTPS/TLS
            if (!req.isSecure()) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "TLS is required");
                return;
            }

            // Retrieve and validate parameters
            String username = req.getParameter("username");
            String password = req.getParameter("password");
            String csrfToken = req.getParameter("csrfToken");
            if (username == null || password == null || csrfToken == null) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing parameters");
                return;
            }

            // Simple whitelist validation: alphanumeric + underscore, 3–50 chars
            if (!username.matches("^[A-Za-z0-9_]{3,50}$")
                    || password.length() < MIN_PASSWORD_LENGTH
                    || password.length() > MAX_PASSWORD_LENGTH) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid credentials format");
                return;
            }

            // CSRF protection: must match token in session
            HttpSession session = req.getSession(false);
            if (session == null
                    || !csrfToken.equals(session.getAttribute("csrfToken"))) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid CSRF token");
                return;
            }

            // Lookup DataSource via JNDI — avoids hardcoding credentials
            DataSource ds;
            try {
                InitialContext ctx = new InitialContext();
                ds = (DataSource) ctx.lookup("java:comp/env/jdbc/MyDB");
            } catch (NamingException e) {
                log("JNDI lookup failed", e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Configuration error");
                return;
            }

            // Retrieve stored password hash for user
            String storedHash = null;
            try (Connection conn = ds.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                         "SELECT password_hash FROM users WHERE username = ? AND active = 1")) {
                ps.setString(1, username);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        storedHash = rs.getString("password_hash");
                    }
                }
            } catch (SQLException e) {
                log("Database error", e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error");
                return;
            }

            // Verify password using constant-time comparison
            if (storedHash == null || !verifyPassword(password, storedHash)) {
                // Do not reveal whether username or password was incorrect
                resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid username or password");
                return;
            }

            // Regenerate session to prevent session fixation
            session.invalidate();
            session = req.getSession(true);
            // Mark session cookie as secure & HttpOnly — most containers do this by default
            session.setAttribute("username", username);
            session.setMaxInactiveInterval(15 * 60);  // 15 minutes

            // Redirect to a post-login page
            resp.sendRedirect(req.getContextPath() + "/dashboard");
        }

        /**
         * Verifies a raw password against a stored PBKDF2WithHmacSHA256 hash.
         * Format of storedHash: iterations:saltBase64:hashBase64
         */
        private static boolean verifyPassword(String password, String storedHash) {
            try {
                String[] parts = storedHash.split(":");
                if (parts.length != 3) {
                    return false;
                }
                int iterations = Integer.parseInt(parts[0]);
                byte[] salt = Base64.getDecoder().decode(parts[1]);
                byte[] expectedHash = Base64.getDecoder().decode(parts[2]);

                PBEKeySpec spec = new PBEKeySpec(
                        password.toCharArray(),
                        salt,
                        iterations,
                        expectedHash.length * 8
                );
                SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
                byte[] calculatedHash = skf.generateSecret(spec).getEncoded();

                // Constant-time comparison to prevent timing attacks
                return MessageDigest.isEqual(expectedHash, calculatedHash);
            } catch (NoSuchAlgorithmException | InvalidKeySpecException | NumberFormatException e) {
                // Log internal error without exposing details to user
                // Using servlet context logger
                // Note: using getServletContext() is not static; if needed, log elsewhere
                return false;
            }
        }
    }
}
