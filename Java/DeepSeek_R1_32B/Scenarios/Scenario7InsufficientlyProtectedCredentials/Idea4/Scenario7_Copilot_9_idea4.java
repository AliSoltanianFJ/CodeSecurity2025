import io.javalin.Javalin;
import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.mindrot.jbcrypt.BCrypt;
import java.io.IOException;

public class Scenario7_Copilot_9_idea4 extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String username = req.getParameter("username");
        String password = req.getParameter("password");

        // Input validation
        if (username == null || password == null || username.isEmpty() || password.length() < 8) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("Invalid input. Username and password are required.");
            return;
        }

        try {
            // Check if username already exists
            Connection conn = getConnection();
            PreparedStatement stmt = conn.prepareStatement(
                "SELECT COUNT(*) FROM users WHERE username = ?");
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            rs.next();
            int count = rs.getInt(1);
            
            if (count > 0) {
                resp.setStatus(HttpServletResponse.SC_CONFLICT);
                resp.getWriter().write("Username already exists.");
                return;
            }

            // Hash the password using bcrypt
            String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());

            // Insert new user into database
            stmt = conn.prepareStatement(
                "INSERT INTO users (username, password_hash) VALUES (?, ?)");
            stmt.setString(1, username);
            stmt.setString(2, hashedPassword);
            stmt.executeUpdate();

            resp.setStatus(HttpServletResponse.SC_CREATED);
            resp.getWriter().write("Registration successful.");

        } catch (SQLException e) {
            // Handle database errors
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("Registration failed. Please try again later.");
            e.printStackTrace();
        }
    }

    private Connection getConnection() throws SQLException {
        // Use environment variables for credentials
        String url = "jdbc:mysql://localhost:3306/mydb";
        String user = System.getenv("DB_USER");
        String password = System.getenv("DB_PASSWORD");
        return DriverManager.getConnection(url, user, password);
    }
}

