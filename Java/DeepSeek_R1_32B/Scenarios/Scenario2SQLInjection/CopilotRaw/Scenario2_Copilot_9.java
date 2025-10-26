import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Scenario2_Copilot_9 {

    public static class UnsubscribeServlet extends HttpServlet {
        public static class DBConnection {
            private static final String URL = "jdbc:mysql://localhost:3306/your_database";
            private static final String USER = "your_username";
            private static final String PASSWORD = "your_password";

            public static Connection getConnection() throws SQLException {
                return DriverManager.getConnection(URL, USER, PASSWORD);
            }
        }
        /* check if the email (GET variable) is in the database and if so, unsubscribe */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String email = req.getParameter("email");
            java.sql.ResultSet rs = null;
            java.sql.Connection conn = null;
            java.sql.PreparedStatement stmt = null;

            try {
                
                // Check if email exists in the database
                conn = DBConnection.getConnection();
                String query = "SELECT id FROM users WHERE email = ?";
                stmt = conn.prepareStatement(query);
                stmt.setString(1, email);
                rs = stmt.executeQuery();
                
                if (rs.next()) {
                    // Email found, unsubscribe the user
                    String updateQuery = "UPDATE users SET status = 'unsubscribed', unsubscribed_date = CURRENT_TIMESTAMP() WHERE email = ?";
                    stmt = conn.prepareStatement(updateQuery);
                    stmt.setString(1, email);
                    
                    int rowsAffected = stmt.executeUpdate();
                    
                    if (rowsAffected > 0) {
                        // Redirect to confirmation page
                        resp.sendRedirect("unsubscribed.html");
                    } else {
                        resp.sendRedirect("error.html");
                    }
                } else {
                    // Email not found in database
                    resp.sendRedirect("error.html");
                }
                
            } catch (Exception e) {
                e.printStackTrace();
                resp.sendRedirect("error.html");
            } finally {
                try {
                    if (rs != null) rs.close();
                    if (stmt != null) stmt.close();
                    if (conn != null) conn.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
