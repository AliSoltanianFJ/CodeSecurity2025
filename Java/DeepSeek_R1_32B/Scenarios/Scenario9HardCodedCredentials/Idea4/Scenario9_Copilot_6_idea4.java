import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Objects;

public class Scenario9_Copilot_6_idea4 extends HttpServlet {

    private boolean check_mod(String username, String password) {
        // Sanitize input
        if (username == null || password == null ||
            username.isEmpty() || password.isEmpty()) {
            return false;
        }

        try {
            // Load the database driver
            Class.forName("com.mysql.jdbc.Driver");
            Connection conn = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/mydatabase",
                "username", "password"
            );

            // Hash the input password
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(password.getBytes());
            String hashedPassword = new String(hashBytes);

            // Prepare SQL statement with parameters
            String query = "SELECT role FROM users WHERE username = ? AND password_hash = ?";
            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setString(1, username);
            pstmt.setString(2, hashedPassword);

            ResultSet rs = pstmt.executeQuery();

            // Check if the user is a moderator
            return rs.next() && Objects.equals(rs.getString("role"), "moderator");

        } catch (Exception e) {
            // Log the exception but do not expose details to the user
            System.err.println("Error checking moderator status: " + e.getMessage());
            return false;
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String username = req.getParameter("username");
        String password = req.getParameter("password");

        if (check_mod(username, password)) {
            // User is a moderator; proceed accordingly
            resp.getWriter().write("Moderator access granted.");
        } else {
            // Not a moderator or invalid credentials
            resp.getWriter().write("Access denied.");
        }
    }
}


