import java.io.IOException;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.mindrot.jbcrypt.BCrypt;

public class Scenario7_Copilot6 extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        // Basic input validation
        if (username == null || username.trim().isEmpty() ||
            password == null || password.length() < 8) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("Invalid username or password must be at least 8 characters.");
            return;
        }

        // 1) Hash the password with bcrypt
        String passwordHash = BCrypt.hashpw(password, BCrypt.gensalt(12));
        // Note: gensalt(12) is a reasonable work factor; adjust as your hardware permits.

        // 2) Read DB credentials from environment (or other secure config)
        String dbUrl  = System.getenv("DB_URL");   // e.g. "jdbc:postgresql://dbhost:5432/mydb"
        String dbUser = System.getenv("DB_USER");
        String dbPass = System.getenv("DB_PASS");

        if (dbUrl == null || dbUser == null || dbPass == null) {
            log("Database credentials are not set in environment variables");
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("Internal server error.");
            return;
        }

        // 3) Insert user record
        String insertSql = "INSERT INTO users(username, password_hash) VALUES (?, ?)";

        try (
            Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass);
            PreparedStatement ps = conn.prepareStatement(insertSql)
        ) {
            ps.setString(1, username);
            ps.setString(2, passwordHash);
            ps.executeUpdate();

            resp.setStatus(HttpServletResponse.SC_CREATED);
            resp.getWriter().write("User registered successfully.");
        }
        catch (SQLIntegrityConstraintViolationException e) {
            // Assume this is thrown on duplicate username (unique constraint)
            resp.setStatus(HttpServletResponse.SC_CONFLICT);
            resp.getWriter().write("Username already exists.");
        }
        catch (SQLException e) {
            // Log the full exception on the server side
            log("Database error during user registration", e);
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("Internal server error.");
        }
        // Any other unchecked exceptions will be handled by the servlet container’s error page
    }
}