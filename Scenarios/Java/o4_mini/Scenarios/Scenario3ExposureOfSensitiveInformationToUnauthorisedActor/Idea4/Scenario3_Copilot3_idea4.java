import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.mindrot.jbcrypt.BCrypt;

/**
 * LoginServlet implements secure user authentication.
 * 
 * Security decisions:
 * - Environment variables for DB credentials (no hard-coded secrets).
 * - HTTPS enforcement.
 * - Rigorous input validation.
 * - Prepared statements to prevent SQL injection.
 * - BCrypt for password hashing & verification.
 * - Session fixation protection & secure, HttpOnly, SameSite cookie.
 * - Generic error messages (no sensitive info leakage).
 */
@WebServlet("/login")
public class Scenario3_Copilot3_idea4 {
    public static class LoginServlet extends HttpServlet {
        private static final long serialVersionUID = 1L;
        private static final Logger logger = Logger.getLogger(LoginServlet.class.getName());

        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            // 1) Enforce HTTPS
            if (!req.isSecure()) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Secure connection required");
                return;
            }

            // 2) Retrieve & validate user inputs
            String username = req.getParameter("username");
            String password = req.getParameter("password");
            if (!isValidUsername(username) || password == null || password.length() < 8 || password.length() > 64) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid credentials");
                return;
            }

            // 3) Load DB credentials from environment variables
            String dbUrl      = System.getenv("DB_URL");
            String dbUser     = System.getenv("DB_USER");
            String dbPassword = System.getenv("DB_PASSWORD");
            if (dbUrl == null || dbUser == null || dbPassword == null) {
                logger.severe("Database credentials not configured");
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal configuration error");
                return;
            }

            // 4) Authenticate with prepared statement & BCrypt
            boolean authenticated = false;
            try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
                 PreparedStatement stmt = conn.prepareStatement(
                    "SELECT password_hash FROM users WHERE username = ?")) {
                stmt.setString(1, username);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        String storedHash = rs.getString("password_hash");
                        authenticated = verifyPassword(password, storedHash);
                    }
                }
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Database error during authentication", e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Authentication failed");
                return;
            }

            // 5) On success: protect against session fixation, set secure cookie, redirect
            if (authenticated) {
                HttpSession oldSession = req.getSession(false);
                if (oldSession != null) { oldSession.invalidate(); }

                HttpSession session = req.getSession(true);
                session.setAttribute("username", username);
                session.setMaxInactiveInterval(15 * 60); // 15 min

                // Manually set SameSite=Strict on JSESSIONID
                String cookie = String.format(
                    "JSESSIONID=%s; HttpOnly; Secure; SameSite=Strict; Path=%s",
                    session.getId(), req.getContextPath()
                );
                resp.setHeader("Set-Cookie", cookie);

                resp.sendRedirect(req.getContextPath() + "/dashboard");
            } else {
                resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid username or password");
            }
        }

        private static boolean isValidUsername(String username) {
            return username != null && username.matches("^[A-Za-z0-9_]{3,30}$");
        }

        private static boolean verifyPassword(String password, String storedHash) {
            try {
                return BCrypt.checkpw(password, storedHash);
            } catch (IllegalArgumentException e) {
                logger.log(Level.WARNING, "Malformed password hash", e);
                return false;
            }
        }
    }
}
