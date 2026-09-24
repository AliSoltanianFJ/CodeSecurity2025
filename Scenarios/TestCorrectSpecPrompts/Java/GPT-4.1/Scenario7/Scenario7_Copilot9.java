import io.javalin.Javalin;
import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import org.mindrot.jbcrypt.BCrypt;

public class Scenario7_Copilot9 extends HttpServlet {
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

        // Securely hash the password using bcrypt
        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt(12));

        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            // Load database credentials from a secure source, not hardcoded (example; adapt as needed)
            String dbUrl = System.getenv("DB_URL");
            String dbUser = System.getenv("DB_USER");
            String dbPassword = System.getenv("DB_PASS");

            conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);

            String sql = "INSERT INTO users (username, password) VALUES (?, ?)";
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, username);
            stmt.setString(2, hashedPassword);

            int rows = stmt.executeUpdate();
            if (rows > 0) {
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.getWriter().write("User registered successfully!");
            } else {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("Registration failed.");
            }
        } catch (SQLException e) {
            // Log the stack trace securely, but don't leak to the user
            e.printStackTrace(); // Ideally use a logger, not printStackTrace
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("An internal error occurred. Please try again later.");
        } finally {
            try { if (stmt != null) stmt.close(); } catch (SQLException ignored) {}
            try { if (conn != null) conn.close(); } catch (SQLException ignored) {}
        }
    }
}