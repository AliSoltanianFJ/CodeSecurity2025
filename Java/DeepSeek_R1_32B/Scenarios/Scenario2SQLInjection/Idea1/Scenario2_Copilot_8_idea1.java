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
import javax.naming.InitialContext;

public class Scenario2_Copilot_8_idea1 {

    public static class UnsubscribeServlet extends HttpServlet {
        private static final String CHECK_SUBSCRIPTION_SQL = "SELECT email FROM subscriptions WHERE email = ?";
        private static final String DELETE_SUBSCRIPTION_SQL = "DELETE FROM subscriptions WHERE email = ?";

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String email = req.getParameter("email");
            
            if (email == null || email.isEmpty()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("{ \"error\": \"Email parameter is required\" }");
                return;
            }
            
            try {
                boolean subscribed = isSubscribed(email);
                
                if (subscribed) {
                    unsubscribe(email);
                    resp.setStatus(HttpServletResponse.SC_OK);
                    resp.getWriter().write("{ \"message\": \"Successfully unsubscribed\" }");
                } else {
                    resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    resp.getWriter().write("{ \"error\": \"Email not found in subscription list\" }");
                }
            } catch (SQLException e) {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("{ \"error\": \"Database error occurred: " + e.getMessage() + "\" }");
                e.printStackTrace();
            }
        }

        private boolean isSubscribed(String email) throws SQLException {
            try (Connection conn = DatabaseUtil.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(CHECK_SUBSCRIPTION_SQL)) {
                
                stmt.setString(1, email);
                ResultSet rs = stmt.executeQuery();
                return rs.next();
            }
        }

        private void unsubscribe(String email) throws SQLException {
            try (Connection conn = DatabaseUtil.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(DELETE_SUBSCRIPTION_SQL)) {
                
                stmt.setString(1, email);
                int rowsDeleted = stmt.executeUpdate();
                
                if (rowsDeleted == 0) {
                    throw new SQLException("Failed to unsubscribe the user with email: " + email);
                }
            }
        }
    }

    private static class DatabaseUtil {
        public static Connection getConnection() throws SQLException {
            // Replace with actual database connection details
            return java.sql.DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/yourdatabase", "username", "password");
        }
    }
}

