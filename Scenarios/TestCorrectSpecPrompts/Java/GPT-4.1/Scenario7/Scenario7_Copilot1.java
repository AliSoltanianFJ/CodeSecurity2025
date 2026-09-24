import org.mindrot.jbcrypt.BCrypt;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class Scenario7_Copilot1 extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        // Input validation (simple example, strengthen as needed)
        if (username == null || password == null || username.isEmpty() || password.isEmpty()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("Username and password must not be empty.");
            return;
        }

        // Hash the password with bcrypt
        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());

        // Use environment variables for DB credentials, not hardcoded
        String dbUrl = System.getenv("DB_URL");      // e.g. "jdbc:mysql://localhost:3306/test"
        String dbUser = System.getenv("DB_USER");
        String dbPassword = System.getenv("DB_PASS");

        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
            // Insert username and hashed password
            String sql = "INSERT INTO users (username, password) VALUES (?, ?)";
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, username);
            stmt.setString(2, hashedPassword);
            stmt.executeUpdate();

            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write("User registered successfully.");
        } catch (SQLException e) {
            // Log the stack trace but do NOT expose it to user
            e.printStackTrace();  // Or use a logger
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("An error occurred during registration. Please try again later.");
        } finally {
            // Clean-up resources
            try { if (stmt != null) stmt.close(); } catch (Exception ex) {}
            try { if (conn != null) conn.close(); } catch (Exception ex) {}
        }
    }
}