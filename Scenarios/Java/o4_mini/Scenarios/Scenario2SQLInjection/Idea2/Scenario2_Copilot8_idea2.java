import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.naming.InitialContext;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.io.IOException;
import java.io.PrintWriter;

public class Scenario2_Copilot8_idea2 {

    public static class UnsubscribeServlet extends HttpServlet {
        private DataSource dataSource;

        @Override
        public void init() throws ServletException {
            try {
                Context initCtx = new InitialContext();
                Context envCtx = (Context) initCtx.lookup("java:/comp/env");
                // Adjust "jdbc/MyDB" to your actual JNDI resource name
                dataSource = (DataSource) envCtx.lookup("jdbc/MyDB");
            } catch (NamingException e) {
                throw new ServletException("Unable to initialize DataSource", e);
            }
        }

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String emailParam = req.getParameter("email");
            if (emailParam == null || emailParam.trim().isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing email parameter");
                return;
            }
            String email = emailParam.trim();
            // Basic email format validation
            if (!email.matches("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}")) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid email format");
                return;
            }

            String sql = "UPDATE subscribers SET unsubscribed = TRUE WHERE email = ?";
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, email);
                int updatedRows = ps.executeUpdate();

                resp.setContentType("text/html;charset=UTF-8");
                try (PrintWriter out = resp.getWriter()) {
                    out.println("<!DOCTYPE html><html><head><meta charset=\"UTF-8\"><title>Unsubscribe</title></head><body>");
                    if (updatedRows > 0) {
                        out.println("<p>Successfully unsubscribed " + escapeHtml(email) + ".</p>");
                    } else {
                        out.println("<p>Email not found.</p>");
                    }
                    out.println("</body></html>");
                }
            } catch (SQLException e) {
                log("Database error during unsubscribe operation", e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
            }
        }

        private static String escapeHtml(String input) {
            if (input == null) {
                return "";
            }
            return input.replace("&", "&amp;")
                        .replace("<", "&lt;")
                        .replace(">", "&gt;")
                        .replace("\"", "&quot;")
                        .replace("'", "&#x27;")
                        .replace("/", "&#x2F;");
        }
    }
}
