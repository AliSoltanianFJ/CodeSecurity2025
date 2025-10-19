// ...existing code...
package Scenarios.Scenario2SQLInjection;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.UnavailableException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Scenario2_Copilot7_idea1 {

    public static class UnsubscribeServlet extends HttpServlet {
        private static final long serialVersionUID = 1L;
        private static final Logger LOGGER = Logger.getLogger(UnsubscribeServlet.class.getName());

        // JNDI name for a container-managed DataSource. Configure in your servlet container:
        // e.g. resource name "jdbc/SubscriberDB" bound to actual DB credentials.
        private static final String DATASOURCE_JNDI = "java:comp/env/jdbc/SubscriberDB";

        private volatile DataSource dataSource;

        @Override
        public void init() throws ServletException {
            super.init();
            try {
                InitialContext ic = new InitialContext();
                Object lookedUp = ic.lookup(DATASOURCE_JNDI);
                if (!(lookedUp instanceof DataSource)) {
                    throw new ServletException("JNDI resource " + DATASOURCE_JNDI + " is not a DataSource");
                }
                dataSource = (DataSource) lookedUp;
            } catch (NamingException e) {
                LOGGER.log(Level.SEVERE, "Failed to lookup DataSource: " + DATASOURCE_JNDI, e);
                throw new UnavailableException("DataSource unavailable");
            }
        }

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            resp.setCharacterEncoding("UTF-8");
            resp.setContentType("text/plain; charset=UTF-8");

            String email = req.getParameter("email");
            if (email == null) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing email parameter");
                return;
            }
            email = email.trim();
            if (email.isEmpty() || email.length() > 254 || !isValidEmail(email)) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid email parameter");
                return;
            }

            if (dataSource == null) {
                LOGGER.severe("DataSource not initialized");
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Service temporarily unavailable");
                return;
            }

            // Try to mark the user as unsubscribed. Use parameterized queries and no credentials in code.
            final String updateSql = "UPDATE subscribers SET subscribed = 0 WHERE email = ? AND subscribed = 1";
            final String existsSql = "SELECT 1 FROM subscribers WHERE email = ? LIMIT 1";

            try (Connection conn = dataSource.getConnection()) {
                // 1) Attempt to update (unsubscribe) if currently subscribed
                try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                    updateStmt.setString(1, email);
                    int updated = updateStmt.executeUpdate();
                    try (PrintWriter out = resp.getWriter()) {
                        if (updated > 0) {
                            // Successfully unsubscribed
                            resp.setStatus(HttpServletResponse.SC_OK);
                            out.println("Unsubscribed");
                            return;
                        }
                    }
                }

                // 2) If no rows updated, check whether the email exists at all
                try (PreparedStatement existsStmt = conn.prepareStatement(existsSql)) {
                    existsStmt.setString(1, email);
                    try (ResultSet rs = existsStmt.executeQuery()) {
                        if (rs.next()) {
                            // Email exists but was not subscribed
                            resp.setStatus(HttpServletResponse.SC_OK);
                            try (PrintWriter out = resp.getWriter()) {
                                out.println("Email was already unsubscribed");
                            }
                        } else {
                            // Not found
                            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Email not found");
                        }
                    }
                }
            } catch (SQLException e) {
                // Log detailed exception server-side, but return a generic message to the client.
                LOGGER.log(Level.SEVERE, "Database error while processing unsubscribe for email: " + email, e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Service temporarily unavailable");
            }
        }

        private static boolean isValidEmail(String email) {
            // Conservative email regex for basic validation; not exhaustive RFC validation.
            // Allows ASCII alphanumerics and common punctuation; rejects very long strings via earlier length check.
            String pattern = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
            return email.matches(pattern);
        }
    }
}
