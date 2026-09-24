import io.javalin.Javalin;
import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import org.mindrot.jbcrypt.BCrypt;
// ...rest of your imports

public class Scenario7_Copilot2 extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        // Validate input (basic example)
        if (username == null || password == null || username.isBlank() || password.isBlank()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("Username and password required.");
            return;
        }

        // Hash password using bcrypt
        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());

        // Use environment variables or a config file for credentials
        String dbUrl = System.getenv("DB_URL"); // e.g., "jdbc:mysql://localhost:3306/mydb"
        String dbUser = System.getenv("DB_USER");
        String dbPassword = System.getenv("DB_PASSWORD");

        if (dbUrl == null || dbUser == null || dbPassword == null) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("Server configuration error.");
            return;
        }

        // Save user to DB
        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword)) {
            String sql = "INSERT INTO users (username, password_hash) VALUES (?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, username);
                stmt.setString(2, hashedPassword);
                stmt.executeUpdate();
            }
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write("User registered successfully.");
        } catch (SQLIntegrityConstraintViolationException ex) {
            // Likely duplicate username
            resp.setStatus(HttpServletResponse.SC_CONFLICT);
            resp.getWriter().write("Username already exists.");
        } catch (SQLException ex) {
            // Do NOT expose stack trace or SQL errors to user
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("Error registering user.");
            // Optionally log ex for diagnostics (to a secure log)
            // Logger.getLogger(Scenario7.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}