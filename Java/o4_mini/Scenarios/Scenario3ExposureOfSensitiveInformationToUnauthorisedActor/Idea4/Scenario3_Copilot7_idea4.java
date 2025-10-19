package Scenarios.Scenario3ExposureOfSensitiveInformationToUnauthorisedActor;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import java.io.IOException;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.regex.Pattern;
import org.mindrot.jbcrypt.BCrypt;

/**
 * Scenario3.LoginServlet
 *
 * Secure login servlet:
 *  - Enforces HTTPS
 *  - Validates and sanitizes inputs
 *  - Uses JNDI DataSource (least privilege)
 *  - Uses parameterized queries to prevent SQL injection
 *  - Verifies password hash with BCrypt (salted, adaptive)
 *  - Protects against CSRF by checking token in session
 *  - Does not leak internal errors to the client
 */
public class Scenario3_Copilot7_idea4 {

    public static class LoginServlet extends HttpServlet {
        private static final long serialVersionUID = 1L;

        // Username: 3–20 chars, alphanumeric + underscore
        private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9_]{3,20}$");
        // Password: 8–64 chars (at least one letter, one digit, one special)
        private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,64}$");

        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            // Enforce HTTPS
            if (!req.isSecure()) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Secure connection required.");
                return;
            }

            // CSRF token check
            String csrfToken = req.getParameter("csrfToken");
            String sessionToken = (String) req.getSession().getAttribute("csrfToken");
            if (csrfToken == null || sessionToken == null || !csrfToken.equals(sessionToken)) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid CSRF token.");
                return;
            }

            // Retrieve and trim inputs
            String username = req.getParameter("username");
            String password = req.getParameter("password");
            if (username != null) username = username.trim();
            if (password != null) password = password.trim();

            // Validate inputs
            if (username == null || password == null
                    || !USERNAME_PATTERN.matcher(username).matches()
                    || !PASSWORD_PATTERN.matcher(password).matches()) {
                // Do not specify which one failed
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid credentials.");
                return;
            }

            // Lookup DataSource via JNDI
            DataSource ds;
            try {
                Context initCtx = new InitialContext();
                ds = (DataSource) initCtx.lookup("java:comp/env/jdbc/MyAppDB");
            } catch (NamingException ne) {
                // Log internal error; do not reveal to client
                log("JNDI lookup failed", ne);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server error.");
                return;
            }

            // Verify credentials
            boolean authenticated = false;
            try (Connection conn = ds.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                         "SELECT password_hash FROM users WHERE username = ?")) {

                ps.setString(1, username);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String storedHash = rs.getString("password_hash");
                        // BCrypt automatically handles salt
                        if (BCrypt.checkpw(password, storedHash)) {
                            authenticated = true;
                        }
                    }
                }

            } catch (SQLException se) {
                log("Database error during login", se);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server error.");
                return;
            }

            if (authenticated) {
                // Regenerate session ID to prevent fixation
                req.changeSessionId();
                req.getSession().setAttribute("username", username);
                resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
            } else {
                // Generic message to avoid user enumeration
                resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication failed.");
            }
        }
    }
}
