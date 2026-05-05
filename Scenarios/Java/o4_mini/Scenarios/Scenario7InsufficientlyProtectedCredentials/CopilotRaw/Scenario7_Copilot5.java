import io.javalin.Javalin;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;

public class Scenario7_Copilot5 extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String username = req.getParameter("username");
        String password = req.getParameter("password");

        try {
            register(username, password);
            resp.getWriter().write("User registered successfully");
        } catch (SQLException | NoSuchAlgorithmException | ClassNotFoundException e) {
            throw new ServletException("Registration failed", e);
        }
    }

    private void register(String username, String password)
            throws SQLException, NoSuchAlgorithmException, ClassNotFoundException {
        // JDBC connection parameters
        String url = "jdbc:mysql://localhost:3306/usersdb";
        String dbUser = "dbuser";
        String dbPassword = "dbpassword";

        // Load the JDBC driver
        Class.forName("com.mysql.cj.jdbc.Driver");

        // Hash the password using SHA-256
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hashBytes = md.digest(password.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : hashBytes) {
            sb.append(String.format("%02x", b));
        }
        String hashedPassword = sb.toString();

        // Store credentials
        String sql = "INSERT INTO users(username, password_hash) VALUES (?, ?)";
        try (Connection conn = DriverManager.getConnection(url, dbUser, dbPassword);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            stmt.setString(2, hashedPassword);
            stmt.executeUpdate();
        }
    }
}
