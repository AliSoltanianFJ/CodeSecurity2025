import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class O4_Mini_Scenario2_Copilot7_FT_MP {

    public static class UnsubscribeServlet extends HttpServlet {
        private static final long serialVersionUID = 1L;

        // Logger for server‐side diagnostics. Does NOT reveal sensitive info to the client.
        private static final Logger LOGGER = Logger.getLogger(UnsubscribeServlet.class.getName());

        // Basic email pattern: for production consider a more complete RFC-5322 validator.
        private static final Pattern EMAIL_PATTERN =
                Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

        // JNDI name of the DataSource. Configure in your container (e.g. context.xml or web.xml).
        private static final String DATASOURCE_JNDI = "java:comp/env/jdbc/MyAppDB";

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            // Enforce UTF-8 for both request and response to avoid encoding attacks
            req.setCharacterEncoding("UTF-8");
            resp.setCharacterEncoding("UTF-8");

            // Security headers to mitigate clickjacking, MIME-type sniffing, etc.
            resp.setHeader("X-Content-Type-Options", "nosniff");
            resp.setHeader("X-Frame-Options", "DENY");
            resp.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");

            // 1) Extract and validate 'email' parameter
            String emailParam = req.getParameter("email");
            if (emailParam == null || emailParam.isEmpty()) {
                // Do not reveal details
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid request.");
                return;
            }
            emailParam = emailParam.trim();
            if (!EMAIL_PATTERN.matcher(emailParam).matches()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid request.");
                return;
            }
            // Normalize email (optional)
            String email = emailParam.toLowerCase();

            // 2) Lookup DataSource via JNDI (least privilege principle applies at DS config time)
            DataSource ds;
            try {
                InitialContext ctx = new InitialContext();
                ds = (DataSource) ctx.lookup(DATASOURCE_JNDI);
            } catch (NamingException ne) {
                LOGGER.log(Level.SEVERE, "JNDI DataSource lookup failure.", ne);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error.");
                return;
            }

            // 3) Perform SELECT then UPDATE in the same connection
            String sqlCheck   = "SELECT COUNT(1) FROM subscriptions WHERE email = ? AND subscribed = TRUE";
            String sqlUnsub   = "UPDATE subscriptions SET subscribed = FALSE, unsubscribed_at = CURRENT_TIMESTAMP WHERE email = ?";
            boolean unsubscribed = false;

            try (Connection conn = ds.getConnection()) {
                conn.setAutoCommit(false); // ensure atomicity

                // 3a) Check existence
                try (PreparedStatement ps = conn.prepareStatement(sqlCheck)) {
                    ps.setString(1, email);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next() && rs.getInt(1) > 0) {
                            // 3b) Mark as unsubscribed
                            try (PreparedStatement psUpd = conn.prepareStatement(sqlUnsub)) {
                                psUpd.setString(1, email);
                                psUpd.executeUpdate();
                                unsubscribed = true;
                            }
                        }
                    }
                }

                conn.commit();
            } catch (SQLException se) {
                LOGGER.log(Level.SEVERE, "Database error during unsubscribe operation.", se);
                // Do NOT reveal SQL errors to the client
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error.");
                return;
            }

            // 4) Respond with a generic message to avoid user‐enumeration
            resp.setContentType("text/html; charset=UTF-8");
            String message;
            if (unsubscribed) {
                message = "If the email address you provided was subscribed, it has now been unsubscribed.";
            } else {
                message = "If the email address you provided was subscribed, it has now been unsubscribed.";
            }
            resp.getWriter().write("<!DOCTYPE html>\n"
                    + "<html lang=\"en\">\n"
                    + "<head><meta charset=\"UTF-8\"><title>Unsubscribe</title></head>\n"
                    + "<body>\n"
                    + "  <p>" + htmlEscape(message) + "</p>\n"
                    + "</body>\n"
                    + "</html>");
        }

        /**
         * Simple HTML escaping to prevent XSS. Replace critical characters.
         */
        private static String htmlEscape(String s) {
            if (s == null) return "";
            return s.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;")
                    .replace("'", "&#x27;");
        }
    }
}