import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import java.io.IOException;
import java.sql.*;
import java.util.regex.Pattern;

public class Scenario2_Copilot5_Idea3 {

    public static class UnsubscribeServlet extends HttpServlet {
        private static final long serialVersionUID = 1L;

        // Simple RFC-5322–ish email regex. For production you may want a more
        // robust validator (or use javax.mail.internet.InternetAddress).
        private static final Pattern EMAIL_REGEX =
                Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", Pattern.CASE_INSENSITIVE);

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String email = req.getParameter("email");
            if (email == null || email.trim().isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing email parameter");
                return;
            }
            email = email.trim();
            if (!EMAIL_REGEX.matcher(email).matches()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid email format");
                return;
            }

            // Look up the DataSource via JNDI (configured in your app server’s context)
            DataSource ds;
            try {
                Context ctx = new InitialContext();
                // Adjust this JNDI name to whatever you have configured
                ds = (DataSource) ctx.lookup("java:comp/env/jdbc/MyAppDS");
            } catch (NamingException ne) {
                log("JNDI lookup failed", ne);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server configuration error");
                return;
            }

            // Now check if the email exists, and if so, unsubscribe
            String checkSql       = "SELECT 1 FROM subscribers WHERE email = ?";
            String unsubscribeSql = "UPDATE subscribers SET subscribed = FALSE WHERE email = ?";

            try (
                Connection       conn  = ds.getConnection();
                PreparedStatement checkStmt = conn.prepareStatement(checkSql);
            ) {
                checkStmt.setString(1, email);
                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (!rs.next()) {
                        // no such email
                        resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Email address not found");
                        return;
                    }
                }

                // email exists → perform unsubscribe
                try (PreparedStatement unsubStmt = conn.prepareStatement(unsubscribeSql)) {
                    unsubStmt.setString(1, email);
                    int updated = unsubStmt.executeUpdate();
                    if (updated == 1) {
                        resp.setStatus(HttpServletResponse.SC_OK);
                        resp.setContentType("text/plain;charset=UTF-8");
                        resp.getWriter().write("You have been successfully unsubscribed.");
                    } else {
                        // This shouldn’t happen if the row existed, but guard anyway
                        resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unable to unsubscribe");
                    }
                }

            } catch (SQLException sqle) {
                log("Database error during unsubscribe", sqle);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error");
            }
        }
    }
}