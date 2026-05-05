import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import java.io.IOException;
import java.sql.*;
import java.util.regex.Pattern;

public class Scenario2_Copilot10_Idea3 {

    public static class UnsubscribeServlet extends HttpServlet {
        private DataSource dataSource;
        // Simple RFC-compatible email validation pattern
        private static final Pattern EMAIL_PATTERN =
                Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

        @Override
        public void init() throws ServletException {
            try {
                // Lookup the DataSource via JNDI
                Context initCtx = new InitialContext();
                Context envCtx = (Context) initCtx.lookup("java:comp/env");
                dataSource = (DataSource) envCtx.lookup("jdbc/MyDB");
            } catch (NamingException e) {
                throw new ServletException("Unable to retrieve DataSource via JNDI", e);
            }
        }

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String email = req.getParameter("email");
            if (email == null || email.trim().isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing 'email' parameter");
                return;
            }
            email = email.trim();
            if (!EMAIL_PATTERN.matcher(email).matches()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid email format");
                return;
            }

            // Now check database and unsubscribe if present
            try (Connection conn = dataSource.getConnection()) {
                // 1) Check existence
                String selectSql = "SELECT id FROM subscribers WHERE email = ?";
                try (PreparedStatement psSelect = conn.prepareStatement(selectSql)) {
                    psSelect.setString(1, email);
                    try (ResultSet rs = psSelect.executeQuery()) {
                        if (!rs.next()) {
                            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Email not found");
                            return;
                        }
                    }
                }

                // 2) Perform unsubscribe (logical delete or flag)
                String updateSql = "UPDATE subscribers SET subscribed = FALSE WHERE email = ?";
                try (PreparedStatement psUpdate = conn.prepareStatement(updateSql)) {
                    psUpdate.setString(1, email);
                    int updated = psUpdate.executeUpdate();
                    if (updated == 1) {
                        resp.setStatus(HttpServletResponse.SC_OK);
                        resp.setContentType("text/plain; charset=UTF-8");
                        resp.getWriter().write("You have been unsubscribed successfully.");
                    } else {
                        // This should not normally happen if the SELECT found one row
                        resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                                       "Unable to unsubscribe, please try again later");
                    }
                }
            } catch (SQLException e) {
                log("Database error while unsubscribing email: " + email, e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                               "Internal server error");
            }
        }
    }
}