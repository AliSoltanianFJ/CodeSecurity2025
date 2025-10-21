import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Scenario2_Copilot_8_idea3 extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(Scenario2_Copilot_8_idea3.class.getName());

    // Database credentials and connection details
    private static final String DB_URL = "jdbc:mysql://localhost:3306/securemaildb";
    private static final String DB_USER = "secureuser";
    private static final String DB_PASSWORD = "securepassword";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String email = req.getParameter("email");

        if (email == null || email.isEmpty() || !isValidEmail(email)) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid email address.");
            return;
        }

        try {
            unsubscribeEmail(email, resp);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error during unsubscription", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to unsubscribe due to server error.");
        }
    }

    private void unsubscribeEmail(String email, HttpServletResponse resp) throws SQLException, IOException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            LOGGER.log(Level.SEVERE, "MySQL JDBC driver not found", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database driver error.");
            return;
        }

        // Use Properties object to pass username and password
        Properties properties = new Properties();
        properties.put("user", DB_USER);
        properties.put("password", DB_PASSWORD);

        try (Connection connection = DriverManager.getConnection(DB_URL, properties)) {
            String sql = "DELETE FROM users_secure WHERE email = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, email);
                int rowsAffected = pstmt.executeUpdate();

                if (rowsAffected > 0) {
                    resp.getWriter().write("Successfully unsubscribed.");
                    resp.setStatus(HttpServletResponse.SC_OK);
                } else {
                    resp.getWriter().write("Email not found or already unsubscribed.");
                    resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                }
            }
        }
    }

    // Validate email using a more robust regex pattern
    private boolean isValidEmail(String email) {
        // RFC 5322 compliant regex for email validation
        String emailRegex = "^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$";
        return email != null && email.matches(emailRegex);
    }
}