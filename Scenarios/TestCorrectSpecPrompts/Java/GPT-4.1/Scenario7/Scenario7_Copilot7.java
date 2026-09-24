import io.javalin.Javalin;
import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.mindrot.jbcrypt.BCrypt;
    
public class Scenario7_Copilot7 extends HttpServlet {
    /* hash the password */

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        if (username == null || password == null || username.isEmpty() || password.isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Username and password must not be empty.");
            return;
        }

        // Hash the password using bcrypt
        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt(12));

        // Use environment variables or a secure config for database credentials
        String dbUrl = System.getenv("DB_URL");
        String dbUser = System.getenv("DB_USER");
        String dbPass = System.getenv("DB_PASS");

        if (dbUrl == null || dbUser == null || dbPass == null) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database not configured properly.");
            return;
        }

        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass)) {
            String sql = "INSERT INTO users (username, password) VALUES (?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, username);
                stmt.setString(2, hashedPassword);

                int rowsInserted = stmt.executeUpdate();
                if (rowsInserted > 0) {
                    resp.setStatus(HttpServletResponse.SC_CREATED);
                    resp.getWriter().write("Registration successful!");
                } else {
                    resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Registration failed.");
                }
            }
        } catch (SQLException e) {
            // Do NOT expose sensitive error details
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "An error occurred. Please try again later.");
            // Optionally log full exception on server side only
            // e.printStackTrace(); 
        }
    }
}
