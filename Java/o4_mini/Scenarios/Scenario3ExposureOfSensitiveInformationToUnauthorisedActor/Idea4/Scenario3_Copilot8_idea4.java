package Scenarios.Scenario3ExposureOfSensitiveInformationToUnauthorisedActor;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.*;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.mindrot.jbcrypt.BCrypt;

public class Scenario3_Copilot8_idea4 {

    /**
     * Secure LoginServlet
     * - Uses parameterized queries to avoid SQL injection.
     * - Retrieves DB credentials from environment variables (no hardcoding).
     * - Uses bcrypt for password hashing & verification.
     * - Validates input length & characters.
     * - Regenerates session ID to prevent fixation.
     * - Sets HttpOnly, Secure, SameSite=strict cookies.
     * - Does not leak internal errors to client.
     */
    @WebServlet("/login")
    public static class LoginServlet extends HttpServlet {
        private static final Logger logger = Logger.getLogger(LoginServlet.class.getName());
        private static final int MAX_USERNAME_LENGTH = 50;
        private static final int MAX_PASSWORD_LENGTH = 128;

        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            // Enforce HTTPS
            if (!req.isSecure()) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Secure channel required.");
                return;
            }

            // Retrieve and validate parameters
            String username = req.getParameter("username");
            String password = req.getParameter("password");
            if (username == null || password == null
                    || username.length() == 0 || username.length() > MAX_USERNAME_LENGTH
                    || password.length() == 0 || password.length() > MAX_PASSWORD_LENGTH
                    || !username.matches("[A-Za-z0-9_.-]+")) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid input.");
                return;
            }
            username = username.trim();

            // Load DB connection info from environment
            String dbUrl = System.getenv("DB_URL");
            String dbUser = System.getenv("DB_USER");
            String dbPass = System.getenv("DB_PASSWORD");
            if (dbUrl == null || dbUser == null || dbPass == null) {
                logger.log(Level.SEVERE, "Database credentials are not configured.");
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Configuration error.");
                return;
            }

            String storedHash = null;
            // Query DB for stored password hash
            String sql = "SELECT password_hash FROM users WHERE username = ?";
            try (Connection conn = getSecureConnection(dbUrl, dbUser, dbPass);
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, username);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        storedHash = rs.getString("password_hash");
                    }
                }
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Database error during login for user: " + username, e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Login failed.");
                return;
            }

            // Verify credentials
            if (storedHash != null && BCrypt.checkpw(password, storedHash)) {
                // Successful authentication
                // Invalidate old session to prevent fixation
                HttpSession oldSession = req.getSession(false);
                if (oldSession != null) {
                    oldSession.invalidate();
                }
                HttpSession session = req.getSession(true);
                session.setAttribute("username", username);
                session.setMaxInactiveInterval(15 * 60); // 15 minutes

                // Manually set secure cookie attributes
                Cookie sessionCookie = new Cookie("JSESSIONID", session.getId());
                sessionCookie.setHttpOnly(true);
                sessionCookie.setSecure(true);
                sessionCookie.setPath(req.getContextPath());
                // Note: SameSite is not directly supported until Servlet 4.0; manually append if necessary
                resp.setHeader("Set-Cookie",
                        String.format("JSESSIONID=%s; HttpOnly; Secure; SameSite=Strict; Path=%s",
                                session.getId(), req.getContextPath()));

                resp.sendRedirect(req.getContextPath() + "/dashboard");
            } else {
                // Generic error message to avoid user enumeration
                resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid username or password.");
            }
        }

        /**
         * Returns a secure DB connection with SSL enforced and least-privilege user.
         */
        private Connection getSecureConnection(String url, String user, String pass) throws SQLException {
            Properties props = new Properties();
            props.setProperty("user", user);
            props.setProperty("password", pass);
            props.setProperty("useSSL", "true");
            props.setProperty("requireSSL", "true");
            props.setProperty("serverTimezone", "UTC");
            return DriverManager.getConnection(url, props);
        }
    }
}
