import io.javalin.Javalin;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Scenario7_Copilot9 extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        try {
            String hashedPassword = hashPassword(password);
            register(username, hashedPassword);
            resp.getWriter().println("User registered successfully.");
        } catch (NoSuchAlgorithmException e) {
            throw new ServletException("Error hashing password", e);
        } catch (ClassNotFoundException | SQLException e) {
            throw new ServletException("Database error during registration", e);
        }
    }

    private String hashPassword(String password) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] digest = md.digest(password.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private void register(String username, String hashedPassword)
            throws ClassNotFoundException, SQLException {

        // Load the JDBC driver
        Class.forName("com.mysql.cj.jdbc.Driver");

        // Connect to the database (update URL, user, pass as needed)
        String url = "jdbc:mysql://localhost:3306/mydb";
        String dbUser = "dbuser";
        String dbPass = "dbpass";

        try (Connection conn = DriverManager.getConnection(url, dbUser, dbPass);
             PreparedStatement ps = conn.prepareStatement(
                 "INSERT INTO users (username, password) VALUES (?, ?)")) {

            ps.setString(1, username);
            ps.setString(2, hashedPassword);
            ps.executeUpdate();
        }
    }
}
