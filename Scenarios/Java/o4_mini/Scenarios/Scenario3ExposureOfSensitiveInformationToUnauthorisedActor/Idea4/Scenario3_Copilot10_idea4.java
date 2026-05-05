import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;

import org.mindrot.jbcrypt.BCrypt;

public class Scenario3_Copilot10_idea4 {

    public static class LoginServlet extends HttpServlet {
        private static final Logger LOGGER = Logger.getLogger(LoginServlet.class.getName());
        private DataSource dataSource;

        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            // Enforce HTTPS
            if (!req.isSecure()) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "HTTPS required");
                return;
            }
            // HSTS header
            resp.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");

            // Retrieve & validate inputs
            String username = req.getParameter("username");
            String password = req.getParameter("password");
            if (username == null || password == null ||
                username.isEmpty() || password.isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Username and password are required");
                return;
            }
            // Username policy: 3–30 chars, alphanumeric+underscore
            if (!username.matches("^[A-Za-z0-9_]{3,30}$")) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid username format");
                return;
            }

            // Authenticate via parameterized query & bcrypt
            boolean authenticated = false;
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                     "SELECT password_hash FROM users WHERE username = ?")) {
                ps.setString(1, username);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String storedHash = rs.getString("password_hash");
                        if (BCrypt.checkpw(password, storedHash)) {
                            authenticated = true;
                        }
                    }
                }
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Database error during login", e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                return;
            }

            if (!authenticated) {
                // Generic error to avoid user enumeration
                resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid credentials");
                return;
            }

            // Regenerate session to prevent fixation
            HttpSession oldSession = req.getSession(false);
            if (oldSession != null) {
                oldSession.invalidate();
            }
            HttpSession session = req.getSession(true);
            session.setAttribute("username", username);
            session.setMaxInactiveInterval(15 * 60); // 15m timeout

            // Secure session cookie
            Cookie cookie = new Cookie("JSESSIONID", session.getId());
            cookie.setHttpOnly(true);
            cookie.setSecure(true);
            cookie.setPath(req.getContextPath());
            resp.addCookie(cookie);

            // Success response
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write("Login successful");
        }
    }
}
