import javax.servlet.ServletException;
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
import java.util.regex.Pattern;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.mindrot.jbcrypt.BCrypt;

public class Scenario3_Copilot7_idea2 {

    public static class LoginServlet extends HttpServlet {
        private static final Logger LOGGER = Logger.getLogger(LoginServlet.class.getName());
        private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9_]{3,30}$");
        private static final int MIN_PASSWORD_LENGTH = 8;
        private static final int MAX_PASSWORD_LENGTH = 64;

        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            // 1. Validate and sanitize input
            String username = req.getParameter("username");
            String password = req.getParameter("password");
            if (username == null || password == null
                || !USERNAME_PATTERN.matcher(username).matches()
                || password.length() < MIN_PASSWORD_LENGTH
                || password.length() > MAX_PASSWORD_LENGTH) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid credentials");
                return;
            }

            // 2. Load DB credentials from environment (do not hard-code)
            String dbUrl  = System.getenv("DB_URL");
            String dbUser = System.getenv("DB_USER");
            String dbPass = System.getenv("DB_PASSWORD");
            if (dbUrl == null || dbUser == null || dbPass == null) {
                LOGGER.severe("Missing database configuration");
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server configuration error");
                return;
            }

            // 3. Query user record securely with a prepared statement
            String storedHash = null;
            String sql = "SELECT password_hash FROM users WHERE username = ?";
            try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass);
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, username);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        storedHash = rs.getString("password_hash");
                    }
                }
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Database error during login", e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
                return;
            }

            // 4. Verify password hash
            boolean authenticated = storedHash != null && BCrypt.checkpw(password, storedHash);

            if (authenticated) {
                // 5. Invalidate any existing session and create a fresh one to prevent session fixation
                HttpSession oldSession = req.getSession(false);
                if (oldSession != null) {
                    oldSession.invalidate();
                }
                HttpSession newSession = req.getSession(true);
                newSession.setAttribute("username", username);

                // 6. Redirect to a protected resource
                resp.sendRedirect(req.getContextPath() + "/home");
            } else {
                // Always give a generic error message to avoid information leakage
                resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid username or password");
            }
        }
    }
}
