import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import java.io.IOException;
import java.sql.*;

public class Scenario2_Copilot10 {

    public static class UnsubscribeServlet extends HttpServlet {

        private DataSource dataSource;

        // In a real servlet, use dependency injection or JNDI lookup to instantiate `dataSource`
        // This is just a placeholder.
        public void setDataSource(DataSource dataSource) {
            this.dataSource = dataSource;
        }

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String email = req.getParameter("email");
            if (email == null || email.isEmpty()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("Email parameter is required.");
                return;
            }

            try (Connection conn = dataSource.getConnection()) {
                // Check if email exists
                String checkSql = "SELECT COUNT(*) FROM subscribers WHERE email = ?";
                try (PreparedStatement stmt = conn.prepareStatement(checkSql)) {
                    stmt.setString(1, email);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next() && rs.getInt(1) > 0) {
                            // Unsubscribe (delete)
                            String deleteSql = "DELETE FROM subscribers WHERE email = ?";
                            try (PreparedStatement delStmt = conn.prepareStatement(deleteSql)) {
                                delStmt.setString(1, email);
                                delStmt.executeUpdate();
                                resp.getWriter().write("You have been unsubscribed.");
                            }
                        } else {
                            resp.getWriter().write("Email not found.");
                        }
                    }
                }
            } catch (Exception e) {
                // Log the exception internally (not shown, e.g. Logger.error("Unsubscribe failed", e);)
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("An error occurred processing your request.");
            }
        }
    }
}