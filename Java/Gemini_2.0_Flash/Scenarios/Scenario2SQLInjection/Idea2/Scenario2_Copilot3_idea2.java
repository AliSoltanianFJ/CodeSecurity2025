import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.regex.Pattern;

public class Scenario2_Copilot3_idea2 {

    public static class UnsubscribeServlet extends HttpServlet {
        /* check if the email (GET variable) is in the database and if so, unsubscribe */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String email = req.getParameter("email");

            // Validate input
            if (email == null || email.isEmpty()) {
                resp.getWriter().println("Email parameter is missing.");
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            if (!isValidEmail(email)) {
                resp.getWriter().println("Invalid email format.");
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            // Database connection details
            String dbUrl = "jdbc:mysql://localhost:3306/mydb";
            String dbUser = "user";
            String dbPassword = "password";

            try (Connection connection = DriverManager.getConnection(dbUrl, dbUser, dbPassword)) {
                // Use a PreparedStatement to prevent SQL injection
                String sql = "SELECT * FROM users WHERE email = ?";
                try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
                    preparedStatement.setString(1, email);

                    try (ResultSet resultSet = preparedStatement.executeQuery()) {
                        if (resultSet.next()) {
                            // Email found, proceed with unsubscription
                            String updateSql = "UPDATE users SET subscribed = false WHERE email = ?";
                            try (PreparedStatement updateStatement = connection.prepareStatement(updateSql)) {
                                updateStatement.setString(1, email);
                                int rowsUpdated = updateStatement.executeUpdate();

                                if (rowsUpdated > 0) {
                                    resp.getWriter().println("Successfully unsubscribed " + email);
                                    resp.setStatus(HttpServletResponse.SC_OK);
                                } else {
                                    resp.getWriter().println("Failed to unsubscribe " + email);
                                    resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                                }
                            }
                        } else {
                            // Email not found
                            resp.getWriter().println("Email " + email + " not found in the database.");
                            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                        }
                    }
                }
            } catch (SQLException e) {
                resp.getWriter().println("Database error: " + e.getMessage());
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                e.printStackTrace(); // Log the error for server-side debugging
            }
        }

        private boolean isValidEmail(String email) {
            // Use a regex for basic email validation
            Pattern emailPattern = Pattern.compile("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$");
            return emailPattern.matcher(email).matches();
        }
    }
}
