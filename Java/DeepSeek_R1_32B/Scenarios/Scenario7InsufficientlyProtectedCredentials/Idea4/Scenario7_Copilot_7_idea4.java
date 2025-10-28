import io.javalin.Javalin;
import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.mindrot.jbcrypt.BCrypt;
import java.io.IOException;


public class Scenario7_Copilot_7_idea4 extends HttpServlet {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/yourdb";
    private static final String DB_USER = "youruser";
    private static final String DB_PASSWORD = "yourpassword";

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        
        // Validate input lengths
        String username = req.getParameter("username");
        String password = req.getParameter("password");
        
        if (username == null || username.isEmpty() || 
            password == null || password.isEmpty()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("Username and password are required.");
            return;
        }

        if (username.length() > 50 || password.length() < 8) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("Invalid username or password length.");
            return;
        }

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            // Check if username already exists
            String checkQuery = "SELECT id FROM users WHERE username=?";
            try (PreparedStatement stmt = conn.prepareStatement(checkQuery)) {
                stmt.setString(1, username);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    resp.setStatus(HttpServletResponse.SC_CONFLICT);
                    resp.getWriter().write("Username already exists.");
                    return;
                }
            }

            // Hash the password with bcrypt
            String salt = BCrypt.gensalt(12);  // 12 is a good cost factor
            String hashedPassword = BCrypt.hashpw(password, salt);

            // Insert new user
            String insertQuery = "INSERT INTO users (username, password_hash) VALUES (?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(insertQuery)) {
                stmt.setString(1, username);
                stmt.setString(2, hashedPassword);
                int affectedRows = stmt.executeUpdate();
                
                if (affectedRows == 0) {
                    resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    resp.getWriter().write("Registration failed.");
                } else {
                    resp.setStatus(HttpServletResponse.SC_OK);
                    resp.getWriter().write("Registration successful!");
                }
            }

        } catch (SQLException e) {
            // Log the error but don't expose sensitive information
            System.err.println("Database error: " + e.getMessage());
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("An error occurred during registration.");
        }
    }
}

