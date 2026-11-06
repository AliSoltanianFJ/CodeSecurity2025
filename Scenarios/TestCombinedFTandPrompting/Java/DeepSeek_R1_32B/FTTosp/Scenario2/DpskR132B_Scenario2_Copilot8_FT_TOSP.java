import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.io.IOException;
import java.util.regex.Pattern;

public class DpskR132B_Scenario2_Copilot8_FT_TOSP extends HttpServlet {
    /* check if the email (GET variable) is in the database and if so, unsubscribe */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // Validate and sanitize the email input
        String email = req.getParameter("email");
        if (email == null || email.isEmpty() || !isValidEmail(email)) {
            resp.getWriter().write("Invalid email address.");
            return;
        }

        // Use parameterized query to prevent SQL injection
        try {
            // Assuming we have a database connection
            Connection conn = getConnection();
            String sql = "DELETE FROM subscribers WHERE email = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, email);
            int rowsAffected = pstmt.executeUpdate();

            if (rowsAffected > 0) {
                resp.getWriter().write("Successfully unsubscribed.");
            } else {
                resp.getWriter().write("Email not found in the subscription list.");
            }
        } catch (Exception e) {
            // Log the error securely (do not expose details to user)
            // Use a secure logging framework in production
            resp.getWriter().write("Error occurred. Please try again later.");
        }
    }

    // Validate email format
    private boolean isValidEmail(String email) {
        // Simple email validation (not exhaustive, adjust as needed)
        return Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$").matcher(email).matches();
    }

    // Get database connection
    private Connection getConnection() throws Exception {
        // Replace with your database credentials and connection details
        String url = "jdbc:mysql://localhost:3306/mydb";
        String user = "dbuser";
        String password = "dbpassword";
        return DriverManager.getConnection(url, user, password);
    }
}