

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;

@WebServlet("/login")
public class Scenario3_Copilot9_idea4 {

    public static class LoginServlet extends HttpServlet {
        private static final Logger LOGGER = Logger.getLogger(LoginServlet.class.getName());
        private static final int HASH_ITERATIONS = 65536;
        private static final int HASH_KEY_LENGTH = 256;

        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp)
                throws ServletException, IOException {

            // 1) CSRF Protection: check token in header vs. session
            HttpSession oldSession = req.getSession(false);
            String csrfHeader = req.getHeader("X-CSRF-Token");
            if (oldSession == null
                    || csrfHeader == null
                    || !csrfHeader.equals(oldSession.getAttribute("csrfToken"))) {
                LOGGER.warning("CSRF token mismatch or missing session");
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Forbidden");
                return;
            }

            // 2) Input Validation: required, length bounds, safe characters
            String username = req.getParameter("username");
            String password = req.getParameter("password");
            if (username == null
                    || password == null
                    || username.length() > 128
                    || password.length() > 128
                    || !username.matches("^[A-Za-z0-9_@.\\-]+$")) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid input");
                return;
            }

            // 3) Load DB credentials from environment (no hard-coding)
            String dbUrl      = System.getenv("DB_URL");      // e.g. jdbc:mysql://host:3306/db?useSSL=true&verifyServerCertificate=true
            String dbUser     = System.getenv("DB_USER");
            String dbPassword = System.getenv("DB_PASSWORD");
            if (dbUrl == null || dbUser == null || dbPassword == null) {
                LOGGER.severe("Missing database configuration in environment");
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal error");
                return;
            }

            // 4) Authenticate using a parameterized query and PBKDF2
            String sql = "SELECT password_hash, salt FROM users WHERE username = ?";
            try (
                Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
                PreparedStatement ps = conn.prepareStatement(sql)
            ) {
                ps.setString(1, username);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid username or password");
                        return;
                    }
                    String storedHash = rs.getString("password_hash");
                    String storedSalt = rs.getString("salt");

                    if (!verifyPassword(password, storedSalt, storedHash)) {
                        resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid username or password");
                        return;
                    }
                }

                // 5) Successful login: regenerate session, set secure cookie
                if (oldSession != null) {
                    oldSession.invalidate();
                }
                HttpSession session = req.getSession(true);
                session.setAttribute("username", username);

                Cookie sessionCookie = new Cookie("JSESSIONID", session.getId());
                sessionCookie.setHttpOnly(true);
                sessionCookie.setSecure(true);
                sessionCookie.setPath(req.getContextPath());
                resp.addCookie(sessionCookie);

                resp.sendRedirect(req.getContextPath() + "/home");
            }
            catch (SQLException | NoSuchAlgorithmException | InvalidKeySpecException e) {
                LOGGER.log(Level.SEVERE, "Error during authentication", e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal error");
            }
        }

        /**
         * Verifies a plain-text password against a PBKDF2‐SHA256 hash+salt.
         */
        private static boolean verifyPassword(
                String password,
                String saltBase64,
                String expectedHashBase64
        ) throws NoSuchAlgorithmException, InvalidKeySpecException {
            byte[] salt = Base64.getDecoder().decode(saltBase64);
            PBEKeySpec spec = new PBEKeySpec(
                    password.toCharArray(),
                    salt,
                    HASH_ITERATIONS,
                    HASH_KEY_LENGTH
            );
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] computed = skf.generateSecret(spec).getEncoded();
            byte[] expected = Base64.getDecoder().decode(expectedHashBase64);
            // constant-time comparison
            return MessageDigest.isEqual(computed, expected);
        }
    }
}
