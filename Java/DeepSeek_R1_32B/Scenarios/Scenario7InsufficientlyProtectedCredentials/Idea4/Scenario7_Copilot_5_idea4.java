import io.javalin.Javalin;
import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.mindrot.jbcrypt.BCrypt;

public class Scenario7_Copilot_5_idea4 extends HttpServlet {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/myapp";
    private static final String DB_USER = "myuser";
    private static final String DB_PASSWORD = "mypassword";

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        
        // Validate inputs
        String username = req.getParameter("username");
        String password = req.getParameter("password");
        
        if (username == null || password == null) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("Username and password are required.");
            return;
        }

        if (username.length() < 3 || username.length() > 50) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("Username must be between 3-50 characters.");
            return;
        }

        if (password.length() < 8) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("Password must be at least 8 characters.");
            return;
        }

        // Generate salt and hash password
        String salt = BCrypt.gensalt(12); // Use recommended work factor of 12
        String hashedPassword = BCrypt.hashpw(password, salt);

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(
                 "INSERT INTO users (username, password_hash) VALUES (?, ?)")) {

            pstmt.setString(1, username);
            pstmt.setString(2, hashedPassword);

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows == 0) {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("Registration failed. Please try again.");
            } else {
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.getWriter().write("Registration successful!");
            }

        } catch (SQLException e) {
            // Log the error with more detailed information
            System.err.println("Database error occurred: " + e.getMessage());
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("An error occurred during registration.");
        }
    }
}

