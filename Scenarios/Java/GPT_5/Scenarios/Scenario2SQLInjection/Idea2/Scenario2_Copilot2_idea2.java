// ...existing code...
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class Scenario2_Copilot2_idea2 {

    public static class UnsubscribeServlet extends HttpServlet {
        private static final Logger LOG = Logger.getLogger(UnsubscribeServlet.class.getName());

        // Conservative email pattern and length limit to avoid DoS / extremely long inputs
        private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
        private static final int MAX_EMAIL_LENGTH = 254;

        /**
         * Unsubscribe by email (GET parameter "email").
         *
         * Security / correctness measures:
         * - Validate input (presence, length, simple pattern)
         * - Parameterized queries (PreparedStatement) to prevent SQL injection
         * - Use JNDI DataSource (java:comp/env/jdbc/MyDB) and try-with-resources for safe resource management
         * - Do not reveal whether a specific email exists (prevents account enumeration)
         * - Escape any user-derived content before including it in HTML responses (minimal HTML encoding provided)
         * - Avoid logging private data (no plaintext emails in logs)
         */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            // Force UTF-8 handling
            req.setCharacterEncoding("UTF-8");
            resp.setCharacterEncoding("UTF-8");
            resp.setContentType("text/html; charset=UTF-8");

            String email = req.getParameter("email");
            if (email == null) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                try (PrintWriter w = resp.getWriter()) {
                    w.write(htmlEncode("Missing required parameter: email"));
                }
                return;
            }

            email = email.trim();
            if (email.length() == 0 || email.length() > MAX_EMAIL_LENGTH || !EMAIL_PATTERN.matcher(email).matches()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                try (PrintWriter w = resp.getWriter()) {
                    w.write(htmlEncode("Invalid email address"));
                }
                return;
            }

            // Do not echo the email back directly to avoid exposing PII; return a generic response.
            // Use JNDI DataSource. Adjust the JNDI name in your container configuration if needed.
            DataSource ds;
            try {
                InitialContext ctx = new InitialContext();
                ds = (DataSource) ctx.lookup("java:comp/env/jdbc/MyDB");
            } catch (NamingException ne) {
                LOG.log(Level.SEVERE, "DataSource lookup failed", ne);
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                try (PrintWriter w = resp.getWriter()) {
                    w.write(htmlEncode("Server configuration error"));
                }
                return;
            }

            // Perform DB update in a transaction. Use parameterized queries.
            String selectSql = "SELECT id FROM users WHERE email = ?";
            String updateSql = "UPDATE users SET unsubscribed = ? WHERE id = ?";

            try (Connection conn = ds.getConnection()) {
                // Use a short-lived transaction
                conn.setAutoCommit(false);
                try (PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {
                    selectStmt.setString(1, email);
                    try (ResultSet rs = selectStmt.executeQuery()) {
                        if (rs.next()) {
                            long userId = rs.getLong("id");
                            try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                                updateStmt.setBoolean(1, true);
                                updateStmt.setLong(2, userId);
                                int updated = updateStmt.executeUpdate();
                                // commit whether or not rows were changed; consistent state
                                conn.commit();
                                LOG.log(Level.INFO, "Processed unsubscribe request (userId masked)."); // do not log email
                                // Generic response to avoid revealing whether account existed
                                resp.setStatus(HttpServletResponse.SC_OK);
                                try (PrintWriter w = resp.getWriter()) {
                                    w.write(htmlEncode("If the address exists, it has been unsubscribed."));
                                }
                                return;
                            }
                        } else {
                            // No matching email; do not reveal this fact to client
                            conn.commit(); // nothing to change but keep transaction boundaries clean
                            resp.setStatus(HttpServletResponse.SC_OK);
                            try (PrintWriter w = resp.getWriter()) {
                                w.write(htmlEncode("If the address exists, it has been unsubscribed."));
                            }
                            return;
                        }
                    }
                } catch (SQLException sqle) {
                    // rollback on error
                    try {
                        conn.rollback();
                    } catch (SQLException rbe) {
                        LOG.log(Level.WARNING, "Rollback failed", rbe);
                    }
                    LOG.log(Level.SEVERE, "Database error while processing unsubscribe", sqle);
                    resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    try (PrintWriter w = resp.getWriter()) {
                        w.write(htmlEncode("Server error while processing request"));
                    }
                    return;
                } finally {
                    // Restore default auto-commit for safety (connection will be closed by try-with-resources)
                    try {
                        conn.setAutoCommit(true);
                    } catch (SQLException ignore) {
                        // ignore
                    }
                }
            } catch (SQLException e) {
                LOG.log(Level.SEVERE, "Unable to obtain database connection", e);
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                try (PrintWriter w = resp.getWriter()) {
                    w.write(htmlEncode("Server error"));
                }
            }
        }

        // Minimal HTML encoder to avoid XSS if we ever include user-controlled values in an HTML response.
        private static String htmlEncode(String s) {
            if (s == null) return "";
            StringBuilder sb = new StringBuilder(s.length() + 16);
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                switch (c) {
                    case '&':
                        sb.append("&amp;");
                        break;
                    case '<':
                        sb.append("&lt;");
                        break;
                    case '>':
                        sb.append("&gt;");
                        break;
                    case '"':
                        sb.append("&quot;");
                        break;
                    case '\'':
                        sb.append("&#x27;");
                        break;
                    case '/':
                        sb.append("&#x2F;");
                        break;
                    default:
                        sb.append(c);
                }
            }
            return sb.toString();
        }
    }
}
// ...existing code...
