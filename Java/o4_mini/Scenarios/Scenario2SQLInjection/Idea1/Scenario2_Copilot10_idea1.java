import javax.naming.InitialContext;
import javax.naming.NamingException;
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

public class Scenario2_Copilot10_idea1 {

    public static class UnsubscribeServlet extends HttpServlet {
        private static final Logger LOG = Logger.getLogger(UnsubscribeServlet.class.getName());

        /* check if the email (GET variable) is in the database and if so, unsubscribe */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            resp.setContentType("text/plain; charset=UTF-8");

            String email = req.getParameter("email");
            if (email == null) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing email parameter");
                return;
            }
            email = email.trim();
            if (email.isEmpty() || email.length() > 254) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid email parameter");
                return;
            }
            // Simple syntactic validation to reject obviously invalid input.
            if (!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid email parameter");
                return;
            }

            DataSource ds;
            try {
                // Lookup a DataSource configured in the container. Adjust the JNDI name to your environment.
                InitialContext ctx = new InitialContext();
                ds = (DataSource) ctx.lookup("java:comp/env/jdbc/Subscribers");
            } catch (NamingException ne) {
                LOG.log(Level.SEVERE, "DataSource lookup failed", ne);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server configuration error");
                return;
            }

            // Use prepared statements to avoid SQL injection. Do not echo the raw email back to the client.
            final String checkSql = "SELECT 1 FROM subscribers WHERE email = ? AND subscribed = 1 LIMIT 1";
            final String unsubscribeSql = "UPDATE subscribers SET subscribed = 0 WHERE email = ?";

            try (Connection conn = ds.getConnection()) {
                conn.setAutoCommit(false);
                boolean presentAndSubscribed;
                try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                    checkStmt.setString(1, email);
                    try (ResultSet rs = checkStmt.executeQuery()) {
                        presentAndSubscribed = rs.next();
                    }
                }

                try (PrintWriter out = resp.getWriter()) {
                    if (!presentAndSubscribed) {
                        // Do not reveal whether the email exists in the system; provide a generic message.
                        out.println("If this address was subscribed, it has been processed.");
                        conn.rollback(); // nothing changed
                        return;
                    }

                    try (PreparedStatement unsubStmt = conn.prepareStatement(unsubscribeSql)) {
                        unsubStmt.setString(1, email);
                        int updated = unsubStmt.executeUpdate();
                        if (updated > 0) {
                            conn.commit();
                            out.println("Unsubscription processed.");
                        } else {
                            conn.rollback();
                            LOG.log(Level.WARNING, "Expected to update 1 row but updated {0} rows for email", updated);
                            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unable to process request");
                        }
                    }
                }
            } catch (SQLException sqle) {
                LOG.log(Level.SEVERE, "Database error while unsubscribing", sqle);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error");
            }
        }
    }
}
