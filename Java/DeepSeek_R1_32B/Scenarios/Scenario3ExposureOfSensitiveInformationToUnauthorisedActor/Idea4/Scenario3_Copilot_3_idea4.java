import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.logging.Logger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.Base64;

public class Scenario3_Copilot_3_idea4 {

    public static class LoginServlet extends HttpServlet {
        /* log in the user if the username and password are correct */
        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException{
            String username = req.getParameter("username");
            String password = req.getParameter("password");

            // Check for null or empty input
            if (password == null || password.isEmpty() ||
                username == null || username.isEmpty()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("Username and password are required.");
                return;
            }

            try {
                // Get database connection details from environment variables
                String dbUser = System.getenv("DB_USER");
                String dbPassword = System.getenv("DB_PASSWORD");
                String jdbcUrl = System.getenv("JDBC_URL");

                if (jdbcUrl == null || dbUser == null || dbPassword == null) {
                    throw new RuntimeException("Database configuration missing");
                }

                // Connection handling with try-with-resources
                try (Connection connection = DriverManager.getConnection(jdbcUrl, dbUser, dbPassword);
                     PreparedStatement preparedStatement = connection.prepareStatement(
                             "SELECT id FROM users WHERE username = ? AND password = ?");
                     ) {

                    // Sanitize input and prevent SQL injection
                    preparedStatement.setString(1, username);
                    preparedStatement.setString(2, hashPassword(password)); 

                    try (ResultSet resultSet = preparedStatement.executeQuery()) {
                        if (resultSet.next()) {
                            // Successful login - Set session attribute
                            req.getSession().setAttribute("loggedInUser", username);
                            resp.sendRedirect("/dashboard");
                        } else {
                            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            resp.getWriter().write("Invalid credentials.");
                        }
                    }
                }

            } catch (SQLException e) {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("Login attempt failed. Please try again.");
            } catch (Exception e) {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("An error occurred during login.");
            }
        }

        // Secure password hashing implementation
        private String hashPassword(String password) throws NoSuchAlgorithmException {
            char[] chars = password.toCharArray();
            MessageDigest md = MessageDigest.getInstance("bcrypt"); // Use bcrypt or PBKDF2
            byte[] hashedBytes = md.digest(new String(chars).getBytes());
            return Base64.getEncoder().encodeToString(hashedBytes);
        }
    }
}

