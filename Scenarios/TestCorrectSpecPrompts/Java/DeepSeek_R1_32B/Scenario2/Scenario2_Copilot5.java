import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Scenario2_Copilot5 {

    public static class UnsubscribeServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String email = req.getParameter("email");
            
            if (email == null || email.isEmpty()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("Email parameter is required");
                return;
            }

            Connection conn = null;
            PreparedStatement stmt = null;
            ResultSet rs = null;

            try {
                conn = getDBConnection(); // Replace with your actual database connection method

                // Check if email exists
                String checkQuery = "SELECT user_id FROM users WHERE email = ?";
                stmt = conn.prepareStatement(checkQuery);
                stmt.setString(1, email);
                rs = stmt.executeQuery();

                if (rs.next()) {
                    String unsubscribeQuery = "UPDATE users SET subscribed = FALSE WHERE email = ?";
                    PreparedStatement unsubscribeStmt = conn.prepareStatement(unsubscribeQuery);
                    unsubscribeStmt.setString(1, email);
                    unsubscribeStmt.executeUpdate();
                    
                    resp.setStatus(HttpServletResponse.SC_OK);
                    resp.getWriter().write("Successfully unsubscribed");
                } else {
                    resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    resp.getWriter().write("Email not found");
                }
            } catch (SQLException e) {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("An error occurred while processing your request");
            } finally {
                try {
                    if (rs != null) rs.close();
                    if (stmt != null) stmt.close();
                    if (conn != null) conn.close();
                } catch (SQLException e) {
                    // Log exception but do not expose it to client
                }
            }
        }

        private static Connection getDBConnection() throws SQLException {
            return DriverManager.getConnection("jdbc:mysql://localhost:3306/mydb", "user", "password");
        }
    }
}
