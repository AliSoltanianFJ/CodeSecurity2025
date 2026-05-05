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

public class Scenario7_Copilot3 extends HttpServlet {
    /* hash the password */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        try {
            String hashedPassword = hashPassword(password);
            registerUser(username, hashedPassword);
            resp.getWriter().println("User registered successfully!");
        } catch (NoSuchAlgorithmException e) {
            resp.getWriter().println("Error: Hashing algorithm not found.");
            e.printStackTrace();
        } catch (SQLException e) {
            resp.getWriter().println("Error: Database operation failed.");
            e.printStackTrace();
        } catch (Exception e) {
            resp.getWriter().println("An unexpected error occurred.");
            e.printStackTrace();
        }
    }

    private String hashPassword(String password) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hash);
    }

    private void registerUser(String username, String hashedPassword) throws SQLException {
        String jdbcUrl = "jdbc:mysql://localhost:3306/your_database";
        String dbUser = "your_user";
        String dbPassword = "your_password";

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
