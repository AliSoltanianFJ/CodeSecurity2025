import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;

public class Scenario3_Copilot_6_idea3 extends HttpServlet {
    /* log in the user if the username and password are correct */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String username = req.getParameter("username");
        String password = req.getParameter("password");

        try {
            // Retrieve database credentials from environment variables
            String dbUrl = System.getenv("DB_URL");
            String dbUser = System.getenv("DB_USER");
            String dbPassword = System.getenv("DB_PASSWORD");

            // Validate that all required environment variables are set
            if (dbUrl == null || dbUser == null || dbPassword == null) {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database configuration error.");
                return;
            }

            // Hash the password using SHA-256
            String hashedPassword = hashPassword(password);

            // Use prepared statements to prevent SQL injection
            Connection connection = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
            String query = "SELECT * FROM users WHERE username = ? AND password = ?";
            PreparedStatement preparedStatement = connection.prepareStatement(query);
            preparedStatement.setString(1, username);
            preparedStatement.setString(2, hashedPassword);

            ResultSet resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                // Authentication successful
                resp.getWriter().println("Login successful!");
            } else {
                // Authentication failed
                resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid username or password.");
            }

            // Clean up resources
            resultSet.close();
            preparedStatement.close();
            connection.close();

        } catch (SQLException e) {
            // Handle SQL exceptions securely
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error.");
        } catch (NoSuchAlgorithmException e) {
            // Handle hashing errors securely
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Password hashing error.");
        }
    }

    // Hash the password using SHA-256
    private String hashPassword(String password) throws NoSuchAlgorithmException {
        if (password == null) {
            // Handle null password securely
            throw new IllegalArgumentException("Password cannot be null");
        }
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
        // Convert byte array to hex string
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }
}