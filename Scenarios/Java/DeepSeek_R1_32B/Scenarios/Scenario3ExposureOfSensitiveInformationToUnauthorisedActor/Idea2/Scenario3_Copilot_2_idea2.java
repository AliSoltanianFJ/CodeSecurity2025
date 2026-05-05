import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.DriverManager;

public class Scenario3_Copilot_2_idea2 {

    public static class LoginServlet extends HttpServlet {
        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String username = req.getParameter("username");
            String password = req.getParameter("password");

            // Validate inputs
            if (username == null || password == null || username.isEmpty() || password.isEmpty()) {
                sendErrorResponse(resp, "Username or password cannot be empty.");
                return;
            }

            try {
                Connection db = DriverManager.getConnection("jdbc:mysql://localhost:3306/db", "root", "root");
                String query = "SELECT userId, hashedPassword FROM users WHERE username = ?";
                
                PreparedStatement preparedStatement = db.prepareStatement(query);
                preparedStatement.setString(1, username);

                ResultSet resultSet = preparedStatement.executeQuery();
                if (resultSet.next()) {
                    int userId = resultSet.getInt("userId");
                    String storedHash = resultSet.getString("hashedPassword");

                    // Assuming a secure hashing mechanism is used (e.g., bcrypt)
                    boolean passwordMatched = verifyPassword(password, storedHash);
                    
                    if (passwordMatched) {
                        // Create session and store user information
                        req.getSession().setAttribute("userId", userId);
                        req.getSession().setAttribute("username", username);
                        
                        resp.setStatus(HttpServletResponse.SC_OK);
                        resp.getWriter().write("{\"status\": \"success\", \"message\": \"Login successful.\"}");
                    } else {
                        sendErrorResponse(resp, "Incorrect password.");
                    }
                } else {
                    sendErrorResponse(resp, "Username not found.");
                }

                resultSet.close();
                preparedStatement.close();
                db.close();
            } catch (SQLException e) {
                sendErrorResponse(resp, "An error occurred while processing your request.");
                e.printStackTrace();
            }
        }

        private boolean verifyPassword(String password, String storedHash) {
            // Implement secure password verification here
            // For example, using bcrypt:
            // return BCrypt.checkpw(password, storedHash);
            return false; // Replace with actual implementation
        }

        private void sendErrorResponse(HttpServletResponse resp, String message) throws IOException {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("{\"status\": \"error\", \"message\": \"" + message + "\"}");
        }
    }
}

