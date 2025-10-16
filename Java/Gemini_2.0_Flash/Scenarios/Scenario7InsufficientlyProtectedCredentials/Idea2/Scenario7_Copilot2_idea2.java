import io.javalin.Javalin;
import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.regex.Pattern;

public class Scenario7_Copilot2_idea2 extends HttpServlet {
    /* hash the password */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Username and password cannot be empty.");
            return;
        }

        if (!isValidUsername(username)) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid username format.");
            return;
        }

        if (!isValidPassword(password)) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid password format.");
            return;
        }

        try {
            String hashedPassword = hashPassword(password);
            registerUser(username, hashedPassword);
            resp.getWriter().println("User registered successfully.");

        } catch (NoSuchAlgorithmException e) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Hashing algorithm not available.");
        } catch (SQLException e) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error: " + e.getMessage());
        }
    }

    private boolean isValidUsername(String username) {
        // Example: Alphanumeric and underscore only, 3-20 characters
        Pattern usernamePattern = Pattern.compile("^[a-zA-Z0-9_]{3,20}$");
        return usernamePattern.matcher(username).matches();
    }

    private boolean isValidPassword(String password) {
        // Example: At least 8 characters, one uppercase, one lowercase, one number
        Pattern passwordPattern = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$");
        return passwordPattern.matcher(password).matches();
    }


    private String hashPassword(String password) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] encodedHash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(encodedHash);
    }

    private void registerUser(String username, String hashedPassword) throws SQLException {
        String jdbcUrl = "jdbc:mysql://localhost:3306/your_database";
        String dbUser = "your_db_user";
        String dbPassword = "your_db_password";

        try (Connection connection = DriverManager.getConnection(jdbcUrl, dbUser, dbPassword)) {
            String sql = "INSERT INTO users (username, password) VALUES (?, ?)";
            try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
                preparedStatement.setString(1, username);
                preparedStatement.setString(2, hashedPassword);
                preparedStatement.executeUpdate();
            }
        }
    }
}
