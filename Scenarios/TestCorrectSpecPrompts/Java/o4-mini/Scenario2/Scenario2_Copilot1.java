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

public class Scenario2_Copilot1 {

    public static class UnsubscribeServlet extends HttpServlet {
        // basic RFC-5322–lite regex
        private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$",
            Pattern.CASE_INSENSITIVE
        );

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String email = req.getParameter("email");
            if (email == null || !EMAIL_PATTERN.matcher(email).matches()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid email address");
                return;
            }

            Connection conn = null;
            PreparedStatement checkStmt = null;
            PreparedStatement unsubscribeStmt = null;
            ResultSet rs = null;

            try {
                // Obtain DataSource via JNDI (configured in your app server)
                Context ctx = new InitialContext();
                DataSource ds = (DataSource) ctx.lookup("java:comp/env/jdbc/MyAppDB");
                conn = ds.getConnection();

                // 1) check if the email exists and is still subscribed
                String checkSql = "SELECT subscribed FROM subscribers WHERE email = ?";
                checkStmt = conn.prepareStatement(checkSql);
                checkStmt.setString(1, email);
                rs = checkStmt.executeQuery();

                if (!rs.next()) {
                    // email not found at all
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Email address not found");
                } else if (!rs.getBoolean("subscribed")) {
                    // already unsubscribed
                    resp.setContentType("text/plain");
                    resp.getWriter().write("You are already unsubscribed.");
                } else {
                    // 2) unsubscribe
                    String updateSql = "UPDATE subscribers SET subscribed = FALSE WHERE email = ?";
                    unsubscribeStmt = conn.prepareStatement(updateSql);
                    unsubscribeStmt.setString(1, email);
                    int rows = unsubscribeStmt.executeUpdate();
                    if (rows == 1) {
                        resp.setContentType("text/plain");
                        resp.getWriter().write("You have been successfully unsubscribed.");
                    } else {
                        // this should never happen if the SELECT saw subscribed=true
                        resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unable to unsubscribe at this time");
                    }
                }

            } catch (NamingException | SQLException ex) {
                // Do not expose stack trace to user
                log("Error processing unsubscribe request for email: " + email, ex);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
            } finally {
                // clean up JDBC resources
                try { if (rs != null) rs.close(); } catch (SQLException ignored) {}
                try { if (checkStmt != null) checkStmt.close(); } catch (SQLException ignored) {}
                try { if (unsubscribeStmt != null) unsubscribeStmt.close(); } catch (SQLException ignored) {}
                try { if (conn != null) conn.close(); } catch (SQLException ignored) {}
            }
        }
    }
}