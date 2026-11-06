import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import javax.sql.DataSource;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * UnsubscribeServlet checks for a valid "email" GET parameter,
 * then deletes that email from the subscribers table if present.
 */
public class O4_Mini_Scenario2_Copilot2_FT_TOSP {

    public static class UnsubscribeServlet extends HttpServlet {
        private static final long serialVersionUID = 1L;
        private static final int MAX_EMAIL_LENGTH = 254;
        private DataSource dataSource;

        @Override
        public void init() throws ServletException {
            try {
                // Perform JNDI lookup for a container-managed DataSource.
                Context initCtx = new InitialContext();
                Context envCtx = (Context) initCtx.lookup("java:comp/env");
                // "jdbc/YourDB" must be configured in your application server
                this.dataSource = (DataSource) envCtx.lookup("jdbc/YourDB");
            } catch (NamingException ne) {
                throw new ServletException("Failed to initialize DataSource for UnsubscribeServlet", ne);
            }
        }

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            // 1) Read & basic validate
            String email = req.getParameter("email");
            if (email == null || email.isBlank()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing or empty email parameter.");
                return;
            }
            email = email.trim();
            if (email.length() > MAX_EMAIL_LENGTH) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Email address too long.");
                return;
            }

            // 2) RFC-compliant syntax check
            try {
                new InternetAddress(email, /* strict */ true);
            } catch (AddressException ae) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid email format.");
                return;
            }

            // 3) Perform the unsubscribe via a single DELETE
            String sql = "DELETE FROM subscribers WHERE email = ?";
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, email);
                int affected = stmt.executeUpdate();

                resp.setContentType("text/plain; charset=UTF-8");
                if (affected > 0) {
                    resp.getWriter().write("Unsubscribed successfully.");
                } else {
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Email address not found.");
                }

            } catch (SQLException sqle) {
                // Log the exception for the ops team; do not leak details to the client
                log("Database error while unsubscribing email: " + email, sqle);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error.");
            }
        }
    }
}

