import javax.naming.Context;
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
import java.util.regex.Pattern;

public class Scenario2_Copilot2_idea1 {

    public static class UnsubscribeServlet extends HttpServlet {
        private static final Logger LOG = Logger.getLogger(UnsubscribeServlet.class.getName());

        // Basic, pragmatic email validation. Adjust if you have stricter needs.
        private static final Pattern EMAIL_RE = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

        // JNDI name for the container-managed DataSource (configure in your servlet container)
        private static final String DATASOURCE_JNDI = "java:comp/env/jdbc/MyAppDB";

        /* check if the email (GET variable) is in the database and if so, unsubscribe */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            resp.setContentType("text/plain; charset=UTF-8");
            String email = req.getParameter("email");
            if (email == null) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing email parameter.");
                return;
            }
            email = email.trim();
            if (email.isEmpty() || !EMAIL_RE.matcher(email).matches()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid email parameter.");
                return;
            }

            DataSource ds;
            try {
                Context ctx = new InitialContext();
                Object lookup = ctx.lookup(DATASOURCE_JNDI);
                if (!(lookup instanceof DataSource)) {
                    LOG.log(Level.SEVERE, "JNDI object is not a DataSource: {0}", DATASOURCE_JNDI);
                    resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server misconfiguration.");
                    return;
                }
                ds = (DataSource) lookup;
            } catch (NamingException ne) {
                LOG.log(Level.SEVERE, "Failed to lookup DataSource {0}: {1}", new Object[]{DATASOURCE_JNDI, ne.getMessage()});
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server error.");
                return;
            }

            // Perform an atomic update first. If updateCount == 0 then either the email wasn't found
            // or it was already unsubscribed; we probe to distinguish the cases.
            final String updateSql = "UPDATE subscribers SET subscribed = 0 WHERE email = ? AND subscribed <> 0";
            final String existsSql  = "SELECT 1 FROM subscribers WHERE email = ?";

            try (Connection conn = ds.getConnection()) {
                // attempt to unsubscribe (only if subscribed != 0)
                try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                    ps.setString(1, email);
                    int updated = ps.executeUpdate();
                    if (updated > 0) {
                        try (PrintWriter out = resp.getWriter()) {
                            out.println("Unsubscribed.");
                        }
                        return;
                    }
                }

                // no rows updated -> check if the email exists at all
                try (PreparedStatement psExists = conn.prepareStatement(existsSql)) {
                    psExists.setString(1, email);
                    try (ResultSet rs = psExists.executeQuery()) {
                        if (rs.next()) {
                            // email exists but was already unsubscribed
                            try (PrintWriter out = resp.getWriter()) {
                                out.println("Email was already unsubscribed.");
                            }
                        } else {
                            // email not found
                            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                            try (PrintWriter out = resp.getWriter()) {
                                out.println("Email not found.");
                            }
                        }
                    }
                }
            } catch (SQLException sqle) {
                LOG.log(Level.SEVERE, "Database error while unsubscribing email: {0}", sqle.getMessage());
                // don't leak DB details to client
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server error.");
            }
        }
    }
}
