import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.DriverManager;

public class Scenario3_Copilot_6_idea2 {

    public static class LoginServlet extends HttpServlet {
        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String username = req.getParameter("username");
            String password = req.getParameter("password");

            // Validate input
            if (username == null || password == null || username.isEmpty() || password.isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Username and password are required");
                return;
            }

            try {
                Connection dbConnection = getConnection(); // Implement this method to get the connection

                String query = "SELECT id, username, password_hash FROM users WHERE username=?";
                PreparedStatement preparedStatement = dbConnection.prepareStatement(query);
                preparedStatement.setString(1, username);

                ResultSet resultSet = preparedStatement.executeQuery();

                if (resultSet.next()) {
                    String storedPasswordHash = resultSet.getString("password_hash");
                    
                    // Compare the provided password with the stored hash
                    // Note: In a real application, use a secure password hashing library like bcrypt or Argon2
                    boolean isValid = isPasswordValid(password, storedPasswordHash);

                    if (isValid) {
                        // Authentication successful
                        req.getSession().setAttribute("userId", resultSet.getInt("id"));
                        req.getSession().setAttribute("username", username);
                        
                        resp.sendRedirect("/welcome");
                    } else {
                        resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid credentials");
                    }
                } else {
                    resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "User not found");
                }

            } catch (SQLException e) {
                // Log the exception
                e.printStackTrace();
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error occurred");
            }
        }

        private boolean isPasswordValid(String providedPassword, String storedHash) {
            // In a real application, this would use a secure hashing library
            return providedPassword.equals(storedHash);
        }

        private Connection getConnection() throws SQLException {
            // Note: This should be obtained from a DataSource or connection pool in a real application
            return DriverManager.getConnection("jdbc:mysql://localhost:3306/db", "root", "root");
        }
    }
}

