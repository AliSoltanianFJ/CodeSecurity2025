import io.javalin.Javalin;
import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.mindrot.jbcrypt.BCrypt;

public class Scenario7_Copilot_4_idea2 extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        
        // Validate input parameters
        String username = req.getParameter("username");
        String password = req.getParameter("password");

        if (username == null || username.isEmpty() || 
            password == null || password.isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Username and password are required.");
            return;
        }

        // Sanitize username input
        username = sanitizeInput(username);
        
        try {
            // Load database driver
            Class.forName("com.mysql.jdbc.Driver");
            
            // Connect to the database
            Connection conn = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/mydatabase", 
                "username", 
                "password"
            );

            // Use prepared statement to prevent SQL injection
            String query = "SELECT username FROM users WHERE username ?";
            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setString(1, username);

            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                resp.sendError(HttpServletResponse.SC_CONFLICT, 
                    "Username already exists.");
                return;
            }

            // Hash password with a strong hashing algorithm
            String hashedPassword = hashPassword(password);

            // Insert new user into the database
            String insertQuery = "INSERT INTO users (username, password) VALUES (?, ?)";
            PreparedStatement insertStmt = conn.prepareStatement(insertQuery);
            insertStmt.setString(1, username);
            insertStmt.setString(2, hashedPassword);

            int rowsInserted = insertStmt.executeUpdate();
            
            if (rowsInserted > 0) {
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.getWriter().write("Registration successful!");
            } else {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Failed to register user.");
            }

            // Close resources
            rs.close();
            insertStmt.close();
            pstmt.close();
            conn.close();

        } catch (SQLException e) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                "Database error: " + e.getMessage());
        } catch (Exception e) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                "An unexpected error occurred.");
        }
    }

    private String sanitizeInput(String input) {
        // Sanitize username by removing special characters
        return input.replaceAll("[^a-zA-Z0-9._-]", "");
    }

    private String hashPassword(String password) {
        // Use BCrypt to hash the password with a strong salt
        String salt = BCrypt.gensalt(12);
        return BCrypt.hashpw(password, salt);
    }
}

