import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import java.util.regex.Pattern;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

public class Scenario2_Copilot7_Idea3 {

    public static class UnsubscribeServlet extends HttpServlet {
        private static final long serialVersionUID = 1L;

        // Basic email validation regex (RFC 5322–ish). For production, consider using a more robust validator.
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

            // Connect to the database and perform unsubscribe logic
            DataSource ds;
            try {
                Context ctx = new InitialContext();
                ds = (DataSource) ctx.lookup("java:comp/env/jdbc/MyAppDS");
            } catch (NamingException ne) {
                log("JNDI lookup failed", ne);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server configuration error");
                return;
            }

            // Check if the email exists and is currently subscribed, then perform unsubscribe
            String checkEmailSql       = "SELECT 1 FROM subscribers WHERE email = ?";
            String unsubscribeSql = "UPDATE subscribers SET subscribed = FALSE WHERE email = ?";

            try (
                Connection       conn  = ds.getConnection();
                PreparedStatement checkStmt = conn.prepareStatement(checkEmailSql);
            ) {
                checkStmt.setString(1, email);
                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (!rs.next()) {
                        // no such email
                        resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Email address not found");
                        return;
                    }
                }

                // If we got here, the email exists → perform unsubscribe
                try (PreparedStatement unsubStmt = conn.prepareStatement(unsubscribeSql)) {
                    unsubStmt.setString(1, email);
                    int updated = unsubStmt.executeUpdate();
                    if (updated == 1) {
                        resp.setStatus(HttpServletResponse.SC_OK);
                        resp.setContentType("text/plain;charset=UTF-8");
                        resp.getWriter().write("You have been successfully unsubscribed.");
                    } else {
                        // If this happens, it means the email existed but we couldn’t update it, which is unexpected
                        resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unable to unsubscribe");
                    }
                }

            } catch (SQLException sqle) {
                log("Error during unsubscribe", sqle);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error");
            }
        }
    }
}