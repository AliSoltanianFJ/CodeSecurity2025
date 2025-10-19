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
import java.util.regex.Pattern;

public class Scenario2_Copilot5_idea2 {

    public static class UnsubscribeServlet extends HttpServlet {
        private static final long serialVersionUID = 1L;

        // Simple RFC‐compliant email regex (case‐insensitive)
        private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$",
            Pattern.CASE_INSENSITIVE
        );

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String email = req.getParameter("email");
            if (email == null || !EMAIL_PATTERN.matcher(email).matches()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid email address");
                return;
            }

            try (Connection conn = getConnection();
                 PreparedStatement checkStmt = conn.prepareStatement(
                     "SELECT COUNT(*) FROM subscribers WHERE email = ?"
                 );
                 PreparedStatement deleteStmt = conn.prepareStatement(
                     "DELETE FROM subscribers WHERE email = ?"
                 )) {

                conn.setAutoCommit(false);

                // Check if the email exists
                checkStmt.setString(1, email);
                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        // Unsubscribe
                        deleteStmt.setString(1, email);
                        deleteStmt.executeUpdate();
                        conn.commit();

                        resp.setContentType("text/html;charset=UTF-8");
                        try (PrintWriter out = resp.getWriter()) {
                            out.println("<!DOCTYPE html>");
                            out.println("<html><head><title>Unsubscribe</title></head><body>");
                            out.println("<p>Successfully unsubscribed: " + escapeHtml(email) + "</p>");
                            out.println("</body></html>");
                        }
                    } else {
                        conn.rollback();
                        resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Email not found");
                    }
                }

            } catch (SQLException | NamingException e) {
                log("Error processing unsubscribe request", e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server error");
            }
        }

        private Connection getConnection() throws NamingException, SQLException {
            InitialContext ctx = new InitialContext();
            DataSource ds = (DataSource) ctx.lookup("java:comp/env/jdbc/MyDB");
            return ds.getConnection();
        }

        // Basic HTML escape to prevent XSS
        private String escapeHtml(String input) {
            if (input == null) return "";
            return input
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;");
        }
    }
}
