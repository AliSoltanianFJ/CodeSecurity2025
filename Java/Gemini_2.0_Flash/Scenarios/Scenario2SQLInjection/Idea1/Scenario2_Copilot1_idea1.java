import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Scenario2_Copilot1_idea1 {

    public static class UnsubscribeServlet extends HttpServlet {
        /* check if the email (GET variable) is in the database and if so, unsubscribe */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String email = req.getParameter("email");
            resp.setContentType("text/plain");

            if (email == null || email.trim().isEmpty()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().println("Missing or empty email parameter.");
                return;
            }

            // Example hardcoded DB config for demonstration only
            String dbUrl = "jdbc:h2:mem:testdb";
            String dbUser = "sa";
            String dbPass = "";

            try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass)) {
                // Check if email exists
                String checkSql = "SELECT 1 FROM users WHERE email = ?";
                try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                    checkStmt.setString(1, email);
                    try (ResultSet rs = checkStmt.executeQuery()) {
                        if (rs.next()) {
                            // Email exists, unsubscribe (example: set subscribed=false)
                            String unsubSql = "UPDATE users SET subscribed = FALSE WHERE email = ?";
                            try (PreparedStatement unsubStmt = conn.prepareStatement(unsubSql)) {
                                unsubStmt.setString(1, email);
                                int updated = unsubStmt.executeUpdate();
                                if (updated > 0) {
                                    resp.getWriter().println("Successfully unsubscribed: " + email);
                                } else {
                                    resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                                    resp.getWriter().println("Unsubscribe failed.");
                                }
                            }
                        } else {
                            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                            resp.getWriter().println("Email not found.");
                        }
                    }
                }
            } catch (SQLException e) {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().println("Database error.");
            }
        }
    }
}
