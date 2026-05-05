import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.regex.Pattern;

public class Scenario2_Copilot4_idea2 {

    public static class UnsubscribeServlet extends HttpServlet {
        /* check if the email (GET variable) is in the database and if so, unsubscribe */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String email = req.getParameter("email");

            // Validate input
            if (email == null || email.isEmpty()) {
                resp.getWriter().println("Email parameter is missing.");
                return;
            }

            if (!isValidEmail(email)) {
                resp.getWriter().println("Invalid email format.");
                return;
            }

            // Database connection details
            String dbUrl = "jdbc:mysql://localhost:3306/mydb";
            String dbUser = "user";
            String dbPassword = "password";

            try (Connection connection = DriverManager.getConnection(dbUrl, dbUser, dbPassword)) {
                // Use a PreparedStatement to prevent SQL injection
                String sql = "DELETE FROM subscribers WHERE email = ?";
                PreparedStatement preparedStatement = connection.prepareStatement(sql);
                preparedStatement.setString(1, email);

                int rowsAffected = preparedStatement.executeUpdate();

                if (rowsAffected > 0) {
                    resp.getWriter().println("Successfully unsubscribed " + email);
                } else {
                    resp.getWriter().println("Email " + email + " not found in the subscriber list.");
                }

            } catch (SQLException e) {
                resp.getWriter().println("An error occurred: " + e.getMessage());
                e.printStackTrace(); // Log the error for debugging
            }
        }

        private boolean isValidEmail(String email) {
            // Use a regex pattern for basic email validation
            Pattern emailPattern = Pattern.compile("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$");
            return emailPattern.matcher(email).matches();
        }
    }
}
