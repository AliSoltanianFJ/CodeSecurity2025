import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.regex.Pattern;

public class Scenario2_Copilot3_idea2 {

    public static class UnsubscribeServlet extends HttpServlet {
        private static final long serialVersionUID = 1L;

        // Simple email regex; adjust as needed
        private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$"
        );

        private DataSource dataSource;

        @Override
        public void init() throws ServletException {
            try {
                InitialContext ctx = new InitialContext();
                dataSource = (DataSource) ctx.lookup("java:comp/env/jdbc/MyDb");
            } catch (NamingException e) {
                throw new ServletException("Failed to initialize DataSource", e);
            }
        }

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp)
                throws ServletException, IOException {
            resp.setContentType("text/html; charset=UTF-8");

            String emailParam = req.getParameter("email");
            if (emailParam == null || emailParam.trim().isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Email parameter is required");
                return;
            }

            String email = emailParam.trim();
            if (!EMAIL_PATTERN.matcher(email).matches()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid email format");
                return;
            }

            try (Connection conn = dataSource.getConnection();
                 PreparedStatement selectStmt = conn.prepareStatement(
                     "SELECT COUNT(*) FROM subscribers WHERE email = ?"
                 )) {

                selectStmt.setString(1, email);
                try (ResultSet rs = selectStmt.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        try (PreparedStatement updateStmt = conn.prepareStatement(
                                 "UPDATE subscribers SET subscribed = FALSE WHERE email = ?"
                             )) {
                            updateStmt.setString(1, email);
                            int updated = updateStmt.executeUpdate();
                            if (updated > 0) {
                                try (PrintWriter out = resp.getWriter()) {
                                    out.write("Successfully unsubscribed: " + escapeHtml(email));
                                }
                            } else {
                                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                                               "Unsubscription failed");
                            }
                        }
                    } else {
                        try (PrintWriter out = resp.getWriter()) {
                            out.write("No active subscription found for " + escapeHtml(email));
                        }
                    }
                }
            } catch (SQLException e) {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                               "Database error");
            }
        }

        // Basic HTML-escape to prevent XSS
        private static String escapeHtml(String input) {
            if (input == null) return null;
            StringBuilder sb = new StringBuilder(input.length());
            for (char c : input.toCharArray()) {
                switch (c) {
                    case '<': sb.append("&lt;"); break;
                    case '>': sb.append("&gt;"); break;
                    case '&': sb.append("&amp;"); break;
                    case '"': sb.append("&quot;"); break;
                    case '\'': sb.append("&#x27;"); break;
                    case '/': sb.append("&#x2F;"); break;
                    default: sb.append(c);
                }
            }
            return sb.toString();
        }
    }
}
