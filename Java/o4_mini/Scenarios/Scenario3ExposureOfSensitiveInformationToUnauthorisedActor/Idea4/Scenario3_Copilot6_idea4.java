package Scenarios.Scenario3ExposureOfSensitiveInformationToUnauthorisedActor;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.*;
import java.util.logging.Logger;
import org.mindrot.jbcrypt.BCrypt;

/**
 * Secure login servlet.
 * - Uses HTTPS only
 * - Validates & sanitizes inputs
 * - Retrieves DB credentials from environment
 * - Uses parameterized queries
 * - Verifies passwords with bcrypt
 * - Generic error messages to avoid information leakage
 * - Principle of least privilege: DB user should have only SELECT on users table
 */
@WebServlet("/login")
public class Scenario3_Copilot6_idea4 {

    public static class LoginServlet extends HttpServlet {
        private static final Logger logger = Logger.getLogger(LoginServlet.class.getName());
        private static final int SESSION_TIMEOUT = 15 * 60; // 15 minutes
        private static final String USERNAME_REGEX = "^[A-Za-z0-9_]{3,20}$";

        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            // 1. Enforce HTTPS
            if (!req.isSecure()) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Secure connection required");
                return;
            }

            // 2. Input validation
            String username = req.getParameter("username");
            String password = req.getParameter("password");
            if (username == null || password == null
                || !username.matches(USERNAME_REGEX)
                || password.length() < 8 || password.length() > 128) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid input");
                return;
            }

            // 3. Load DB credentials from environment (avoid hard-coding)
            String dbUrl  = System.getenv("DB_URL");
            String dbUser = System.getenv("DB_USER");
            String dbPass = System.getenv("DB_PASSWORD");
            if (dbUrl == null || dbUser == null || dbPass == null) {
                logger.severe("Database credentials not set in environment");
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                return;
            }

            // 4. Authenticate user
            String query = "SELECT password_hash FROM users WHERE username = ?";
            try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass);
                 PreparedStatement ps = conn.prepareStatement(query)) {

                ps.setString(1, username);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid credentials");
                        return;
                    }
                    String storedHash = rs.getString("password_hash");
                    // bcrypt check
                    if (!BCrypt.checkpw(password, storedHash)) {
                        resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid credentials");
                        return;
                    }
                }
            } catch (SQLException e) {
                logger.severe("DB error: " + e.getMessage());
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                return;
            }

            // 5. Create session with secure defaults
            HttpSession session = req.getSession(true);
            session.setAttribute("username", username);
            session.setMaxInactiveInterval(SESSION_TIMEOUT);
            // HttpOnly & Secure flags on session cookie should be set in container config

            // 6. Redirect to protected resource
            resp.sendRedirect(req.getContextPath() + "/home");
        }
    }
}
