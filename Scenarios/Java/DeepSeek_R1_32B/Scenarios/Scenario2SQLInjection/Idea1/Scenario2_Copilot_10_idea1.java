import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.sql.DataSource;
import javax.naming.Context;

public class Scenario2_Copilot_10_idea1 {

    public static class UnsubscribeServlet extends HttpServlet {
        private static final String CHECK_EMAIL_SQL = "SELECT COUNT(*) FROM subscriptions WHERE email = ?";
        private static final String DELETE_EMAIL_SQL = "DELETE FROM subscriptions WHERE email = ?";

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String email = req.getParameter("email");
            
            if (email == null || email.isEmpty()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("{ \"error\": \"Email parameter is required\" }");
                return;
            }

            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement checkStmt = conn.prepareStatement(CHECK_EMAIL_SQL);
                 PreparedStatement deleteStmt = conn.prepareStatement(DELETE_EMAIL_SQL)) {

                // Check if email exists in the database
                checkStmt.setString(1, email);
                ResultSet rs = checkStmt.executeQuery();
                rs.next();
                int count = rs.getInt(1);

                if (count == 0) {
                    resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    resp.getWriter().write("{ \"error\": \"Email not found in subscription list\" }");
                    return;
                }

                // Proceed to delete the email
                deleteStmt.setString(1, email);
                int rowsDeleted = deleteStmt.executeUpdate();

                if (rowsDeleted > 0) {
                    resp.setStatus(HttpServletResponse.SC_OK);
                    resp.getWriter().write("{ \"message\": \"Successfully unsubscribed\" }");
                } else {
                    resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    resp.getWriter().write("{ \"error\": \"Failed to unsubscribe email\" }");
                }

            } catch (SQLException e) {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("{ \"error\": \"Database error occurred: " + e.getMessage() + "\" }");
                throw new IOException("Database operation failed", e);
            }
        }
    }

    private static class DBUtil {
        private static final String URL = "jdbc:mysql://localhost:3306/yourdatabase";
        private static final String USER = "username";
        private static final String PASSWORD = "password";

        public static Connection getConnection() throws SQLException {
            return java.sql.DriverManager.getConnection(URL, USER, PASSWORD);
        }
    }
}

