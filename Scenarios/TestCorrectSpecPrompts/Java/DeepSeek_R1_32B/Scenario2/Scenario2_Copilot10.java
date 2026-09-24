import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Scenario2_Copilot10 {

    public static class UnsubscribeServlet extends HttpServlet {
        /* check if the email (GET variable) is in the database and if so, unsubscribe */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String email = req.getParameter("email");
            
            if (email == null) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("Email parameter is missing");
                return;
            }

            Connection connection = getDatabaseConnection();
            try {
                PreparedStatement stmt = connection.prepareStatement("SELECT id FROM users WHERE email = ?");
                stmt.setString(1, email);
                ResultSet rs = stmt.executeQuery();

                if (rs.next()) {
                    PreparedStatement updateStmt = connection.prepareStatement(
                        "UPDATE users SET subscribed = FALSE WHERE email = ?");
                    updateStmt.setString(1, email);
                    updateStmt.executeUpdate();
                    resp.setStatus(HttpServletResponse.SC_OK);
                    resp.getWriter().write("Unsubscribed successfully!");
                } else {
                    resp.setStatus(HttpServletResponse.SC_OK);
                    resp.getWriter().write("Email not found in the system");
                }
            } catch (SQLException e) {
                // Log the error
                System.out.println("Database error: " + e.getMessage());
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("An error occurred while processing your request");
            } finally {
                // Close database resources if necessary
                // (Assuming connection is managed properly by the connection pool)
            }
        }

        private static Connection getDatabaseConnection() {
            try {
                return DriverManager.getConnection("jdbc:mysql://localhost:3306/mydb", "user", "password");
            } catch (SQLException e) {
                throw new RuntimeException("Failed to get database connection", e);
            }
        }
    }
}
