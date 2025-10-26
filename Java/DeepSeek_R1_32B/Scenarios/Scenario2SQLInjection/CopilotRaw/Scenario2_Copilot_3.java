import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Scenario2_Copilot_3 {

    public static class UnsubscribeServlet extends HttpServlet {
        /* check if the email (GET variable) is in the database and if so, unsubscribe */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String email = req.getParameter("email");
            try {
                // Load the JDBC driver
                Class.forName("com.mysql.jdbc.Driver");

                // Connect to the database
                Connection conn = DriverManager.getConnection(
                        "jdbc:mysql://localhost:3306/yourDatabase", "username", "password");

                // Check if email exists in the subscribers table
                String checkQuery = "SELECT id FROM subscribers WHERE email = ?";
                PreparedStatement pstmtCheck = conn.prepareStatement(checkQuery);
                pstmtCheck.setString(1, email);
                ResultSet rs = pstmtCheck.executeQuery();

                if (rs.next()) {
                    // Email found, proceed to unsubscribe
                    String deleteQuery = "DELETE FROM subscribers WHERE email = ?";
                    PreparedStatement pstmtDelete = conn.prepareStatement(deleteQuery);
                    pstmtDelete.setString(1, email);
                    int rowsAffected = pstmtDelete.executeUpdate();
                    
                    if (rowsAffected > 0) {
                        resp.getWriter().write("Successfully unsubscribed.");
                        resp.setStatus(HttpServletResponse.SC_OK);
                    } else {
                        resp.getWriter().write("Error unsubscribing. Please try again.");
                        resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    }
                } else {
                    // Email not found
                    resp.getWriter().write("Email not found in our records.");
                    resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                }

                // Close resources
                rs.close();
                pstmtCheck.close();
                conn.close();

            } catch (SQLException | ClassNotFoundException e) {
                // Handle database exceptions
                resp.getWriter().write("An error occurred while processing your request.");
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                e.printStackTrace();
            }
        }
    }
}

