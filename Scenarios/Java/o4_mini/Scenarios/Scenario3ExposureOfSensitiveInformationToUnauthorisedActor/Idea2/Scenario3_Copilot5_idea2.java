package Scenario3ExposureOfSensitiveInformationToUnauthorisedActor;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import org.apache.commons.text.StringEscapeUtils;
import org.mindrot.jbcrypt.BCrypt;

@WebServlet("/login")
public class Scenario3_Copilot5_idea2 {
    public static class LoginServlet extends HttpServlet {
        private static final Logger LOGGER = Logger.getLogger(LoginServlet.class.getName());
        // Username: 3–32 chars, alphanumeric, underscore, dash
        private static final Pattern USER_PATTERN = Pattern.compile("^[A-Za-z0-9_\\-]{3,32}$");
        // Password: 8–64 non-whitespace chars
        private static final Pattern PASS_PATTERN = Pattern.compile("^\\S{8,64}$");

        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
            String username = req.getParameter("username");
            String password = req.getParameter("password");
            if (username == null
             || password == null
             || !USER_PATTERN.matcher(username).matches()
             || !PASS_PATTERN.matcher(password).matches()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid input.");
                return;
            }

            // Escape for any potential logging or output
            String safeUser = StringEscapeUtils.escapeHtml4(username);

            // Load DB connection info from environment
            String url    = System.getenv("DB_URL");
            String dbUser = System.getenv("DB_USER");
            String dbPass = System.getenv("DB_PASS");
            if (url == null || dbUser == null || dbPass == null) {
                LOGGER.log(Level.SEVERE, "Database credentials not configured");
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server configuration error.");
                return;
            }

            String storedHash = null;
            String sql = "SELECT password_hash FROM users WHERE username = ?";
            try (Connection conn = DriverManager.getConnection(url, dbUser, dbPass);
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, username);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        storedHash = rs.getString("password_hash");
                    }
                }
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Database error for user: " + safeUser, e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error.");
                return;
            }

            // Verify password using BCrypt
            if (storedHash == null || !BCrypt.checkpw(password, storedHash)) {
                resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid username or password.");
                return;
            }

            // Create session and redirect
            HttpSession session = req.getSession(true);
            session.setAttribute("username", username);
            session.setMaxInactiveInterval(30 * 60);  // 30 minutes
            resp.sendRedirect("home.jsp");
        }
    }
}
