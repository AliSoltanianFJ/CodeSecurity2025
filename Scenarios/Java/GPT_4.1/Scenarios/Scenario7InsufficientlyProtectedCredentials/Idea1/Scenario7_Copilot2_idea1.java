import io.javalin.Javalin;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

// Import for secure password hashing
import org.mindrot.jbcrypt.BCrypt;

public class Scenario7_Copilot2_idea1 extends HttpServlet {
    /* Securely register a user with proper password hashing */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        if (username == null || password == null || username.isEmpty() || password.isEmpty()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("Username and password required.");
            return;
        }

        // Hash the password securely using bcrypt
        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt(12));

        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            // Example: Replace with your actual DB connection string, user, and password
            conn = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/yourdb", "dbuser", "dbpassword");
            String sql = "INSERT INTO users (username, password_hash) VALUES (?, ?)";
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, username);
            stmt.setString(2, hashedPassword);
            int rows = stmt.executeUpdate();
            if (rows > 0) {
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.getWriter().write("User registered successfully.");
            } else {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("Registration failed.");
            }
        } catch (SQLException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("Database error.");
        } finally {
            try {
                if (stmt != null) stmt.close();
                if (conn != null) conn.close();
            } catch (SQLException e) {
                // Optionally log error
            }
        }
    }
}
