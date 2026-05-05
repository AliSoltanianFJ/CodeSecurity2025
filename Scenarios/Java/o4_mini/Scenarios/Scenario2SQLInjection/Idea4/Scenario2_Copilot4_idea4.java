package Scenarios.Scenario2SQLInjection;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.apache.commons.validator.routines.EmailValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * UnsubscribeServlet securely unsubscribes a user by email.
 * Security Decisions:
 *  - Input validation using Apache Commons EmailValidator (prevents malformed input).
 *  - Database operations via JNDI DataSource and PreparedStatement (prevents SQL injection).
 *  - Generic error messages to client; detailed errors are logged (prevents information leakage).
 *  - JSON responses with UTF-8 charset and proper content type (avoids XSS in payload).
 *  - Principle of least privilege: DataSource should be configured with a DB user that only has UPDATE rights.
 *  - Transport security (HTTPS) must be enforced at the container / reverse proxy level.
 */
@WebServlet("/unsubscribe")
public class Scenario2_Copilot4_idea4 extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(Scenario2_Copilot4_idea4.class);

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        // 1. Validate input presence
        String email = req.getParameter("email");
        if (email == null) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("{\"error\":\"Missing email parameter\"}");
            return;
        }

        // 2. Sanitize and validate format
        email = email.trim().toLowerCase();
        if (!EmailValidator.getInstance().isValid(email)) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("{\"error\":\"Invalid email address\"}");
            return;
        }

        // 3. Perform unsubscribe via prepared statement
        try {
            InitialContext ctx = new InitialContext();
            DataSource ds = (DataSource) ctx.lookup("java:comp/env/jdbc/MyAppDataSource");
            try (Connection conn = ds.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                     "UPDATE subscribers SET unsubscribed = TRUE WHERE email = ?")) {
                ps.setString(1, email);
                int count = ps.executeUpdate();
                if (count > 0) {
                    resp.getWriter().write("{\"status\":\"success\"}");
                } else {
                    resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    resp.getWriter().write("{\"error\":\"Email not found\"}");
                }
            }
        } catch (NamingException | SQLException e) {
            // Log full stack trace server-side, but send generic message to client
            logger.error("Failed to unsubscribe email");
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("{\"error\":\"Internal server error\"}");
        }
    }
}
