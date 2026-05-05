import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.*;
import java.util.Properties;
import javax.sql.DataSource;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;

public class Scenario2_Copilot_1_idea3 extends HttpServlet {
    /* check if the email (GET variable) is in the database and if so, unsubscribe */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // Retrieve the 'email' parameter from GET request
        String email = req.getParameter("email");

        // Validate input: check for null or empty email
        if (email == null || email.trim().isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Email address is missing.");
            return;
        }

        // Basic email validation (you might want to use a more robust regex)
        if (!isValidEmail(email)) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid email format.");
            return;
        }

        try {
            // Get database credentials from environment variables or system properties
            String dbUser = System.getenv("DB_USER");
            String dbPassword = System.getenv("DB_PASSWORD");

            // Establish a secure database connection (using JDBC)
            // Note: In production, use proper credential management and encryption.
            // For this example, assume the database URL is hardcoded.
            Class.forName("com.mysql.jdbc.Driver").newInstance();
            java.sql.Connection conn = DriverManager.getConnection("jdbc:mysql://localhost/mydb", dbUser, dbPassword);

            // Prepare SQL query to check if email exists
            String sql = "SELECT id FROM users WHERE email = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, email.trim());

            ResultSet rs = pstmt.executeQuery();

            // Check if the email was found
            if (!rs.next()) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Email not found in subscription list.");
                return;
            }

            int userId = rs.getInt("id");

            // Prepare SQL query to delete the user (unsubscribe)
            sql = "DELETE FROM users WHERE id = ?";
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, userId);

            int rowsAffected = pstmt.executeUpdate();

            if (rowsAffected > 0) {
                resp.getWriter().println("Successfully unsubscribed.");
                resp.setStatus(HttpServletResponse.SC_OK);
            } else {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to unsubscribe due to an internal error.");
            }

        } catch (SQLException e) {
            // Log the exception securely (do not expose details to user)
            // Use a secure logging framework in production
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error. Please try again later.");
        } catch (Exception e) {
            // Handle other exceptions
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "An unexpected error occurred.");
        }
    }

    /**
     * Basic email validation.
     * @param email The email to validate.
     * @return true if valid, false otherwise.
     */
    private boolean isValidEmail(String email) {
        // Simple regex check (not exhaustive)
        return email.contains("@") && email.indexOf('.') > email.indexOf('@');
    }
}