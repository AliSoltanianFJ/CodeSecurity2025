import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.regex.Pattern;

public class Scenario2_Copilot10_idea2 {

    public static class UnsubscribeServlet extends HttpServlet {
        private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

        private DataSource getDataSource() throws NamingException {
            InitialContext ctx = new InitialContext();
            return (DataSource) ctx.lookup("java:comp/env/jdbc/MyDB");
        }

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp)
                throws ServletException, IOException {
            resp.setContentType("text/plain; charset=UTF-8");
            String email = req.getParameter("email");

            // Validate input
            if (email == null || email.isEmpty() || !EMAIL_PATTERN.matcher(email).matches()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().println("Invalid email address.");
                return;
            }

            // Use try-with-resources to prevent resource leaks
            try (Connection conn = getDataSource().getConnection()) {

                // Check if email exists
                String checkSql = "SELECT COUNT(*) FROM subscribers WHERE email = ?";
                try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                    checkStmt.setString(1, email);
                    try (ResultSet rs = checkStmt.executeQuery()) {
                        rs.next();
                        if (rs.getInt(1) == 0) {
                            // Don't reveal whether the email was in the DB
                            resp.getWriter().println("If the email is subscribed, it has been unsubscribed.");
                            return;
                        }
                    }
                }

                // Unsubscribe
                String updateSql = "UPDATE subscribers SET unsubscribed = TRUE WHERE email = ?";
                try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                    updateStmt.setString(1, email);
                    updateStmt.executeUpdate();
                }

                resp.getWriter().println("If the email is subscribed, it has been unsubscribed.");

            } catch (NamingException | SQLException e) {
                log("Error during unsubscribe process", e);
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().println("An internal error occurred.");
            }
        }
    }
}
