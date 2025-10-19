import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Cookie;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import org.mindrot.jbcrypt.BCrypt;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Secure LoginServlet:
 * - Enforces HTTPS
 * - Validates CSRF token
 * - Validates and sanitizes user input
 * - Uses JNDI DataSource (least privilege)
 * - Uses parameterized query
 * - Uses BCrypt for password hashing
 * - Prevents session fixation
 * - Sets Secure/HttpOnly cookie
 * - Emits generic error messages
 * - Adds secure headers
 */
public class Scenario3_Copilot1_idea4 {

    public static class LoginServlet extends HttpServlet {
        private DataSource dataSource;

        @Override
        public void init() throws ServletException {
            try {
                // Lookup the pooled DataSource via JNDI (configured in server, least privilege user)
                Context initCtx = new InitialContext();
                Context envCtx  = (Context) initCtx.lookup("java:comp/env");
                dataSource = (DataSource) envCtx.lookup("jdbc/UserDB");
            } catch (NamingException e) {
                throw new ServletException("Unable to initialize DataSource", e);
            }
        }

        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            // 1) Enforce HTTPS
            if (!req.isSecure()) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Secure connection required.");
                return;
            }

            // 2) Set secure response headers
            resp.setHeader("X-Content-Type-Options", "nosniff");
            resp.setHeader("X-Frame-Options", "DENY");
            resp.setHeader("X-XSS-Protection", "1; mode=block");
            resp.setContentType("text/html; charset=UTF-8");

            // 3) CSRF protection (login form must include a hidden csrf_token)
            HttpSession preSession = req.getSession(false);
            String sessionCsrf = preSession != null
                                 ? (String) preSession.getAttribute("CSRF_TOKEN")
                                 : null;
            String formCsrf    = req.getParameter("csrf_token");
            if (sessionCsrf == null || formCsrf == null || !sessionCsrf.equals(formCsrf)) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid request.");
                return;
            }

            // 4) Fetch & validate parameters
            String username = req.getParameter("username");
            String password = req.getParameter("password");
            if (username == null || password == null
                || username.length() < 3 || username.length() > 50
                || password.length() < 8 || password.length() > 128
                || !username.matches("^[A-Za-z0-9_]+$")) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid credentials.");
                return;
            }

            // 5) Generic error message to avoid user-enumeration
            String genericError = "Invalid username or password.";

            // 6) Authenticate against database
            try (Connection conn = dataSource.getConnection()) {
                String sql = "SELECT password_hash FROM users WHERE username = ?";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, username);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, genericError);
                            return;
                        }
                        String storedHash = rs.getString("password_hash");
                        if (!BCrypt.checkpw(password, storedHash)) {
                            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, genericError);
                            return;
                        }
                    }
                }

                // 7) Successful auth: prevent session fixation
                if (preSession != null) {
                    preSession.invalidate();
                }
                HttpSession session = req.getSession(true);
                session.setAttribute("username", username);
                session.setMaxInactiveInterval(15 * 60); // 15 minutes

                // 8) Issue secure cookie
                Cookie cookie = new Cookie("JSESSIONID", session.getId());
                cookie.setHttpOnly(true);
                cookie.setSecure(true);
                cookie.setPath(req.getContextPath());
                resp.addCookie(cookie);

                // 9) Redirect to landing page
                resp.sendRedirect(req.getContextPath() + "/home");
                return;
            } catch (SQLException e) {
                // 10) Log internally, but return generic 500
                log("Auth error", e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error.");
            }
        }
    }
}
