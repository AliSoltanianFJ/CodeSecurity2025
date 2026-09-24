
import org.mindrot.jbcrypt.BCrypt;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.*;

public class Scenario7_Copilot5 extends HttpServlet {
    /* hash the password */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        if (username == null || password == null || username.isEmpty() || password.isEmpty()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("Username and password are required.");
            return;
        }

        // Hash password securely using bcrypt
        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());

        // Use environment variables or a config file for DB credentials
        String dbUrl = System.getenv("DB_URL");
        String dbUser = System.getenv("DB_USER");
        String dbPassword = System.getenv("DB_PASSWORD");

        if (dbUrl == null || dbUser == null || dbPassword == null) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("Server configuration error.");
            return;
        }

        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword)) {
            String sql = "INSERT INTO users (username, password_hash) VALUES (?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, username);
                stmt.setString(2, hashedPassword);
                stmt.executeUpdate();
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.getWriter().write("Registration successful.");
            }
        } catch (SQLIntegrityConstraintViolationException e) {
            // e.g. duplicate username
            resp.setStatus(HttpServletResponse.SC_CONFLICT);
            resp.getWriter().write("Username already exists.");
        } catch (SQLException e) {
            // Avoid exposing sensitive server info
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("Database error.");
        }
    }
}