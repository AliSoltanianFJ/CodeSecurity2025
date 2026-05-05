// ...existing code...
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.mindrot.jbcrypt.BCrypt;

public class Scenario3_Copilot2_idea1 {

    public static class LoginServlet extends HttpServlet {
        /* log in the user if the username and password are correct */
        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String username = req.getParameter("username");
            String password = req.getParameter("password");

            // Basic input validation
            if (username == null || password == null || username.isEmpty() || password.isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Username and password are required.");
                return;
            }

            // Defend against overly long inputs
            if (username.length() > 100 || password.length() > 100) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid input.");
                return;
            }

            // Use environment variables (or container / JNDI) for DB connection details - do NOT hardcode credentials
            final String DB_URL  = System.getenv("DB_URL");   // e.g. jdbc:mysql://localhost:3306/db
            final String DB_USER = System.getenv("DB_USER");
            final String DB_PASS = System.getenv("DB_PASS");

            if (DB_URL == null || DB_USER == null || DB_PASS == null) {
                log("Database credentials not configured in environment");
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                return;
            }

            // Dummy bcrypt hash to use when a user is not found to mitigate timing/user enumeration
            final String DUMMY_BCRYPT_HASH = "$2a$10$7EqJtq98hPqEX7fNZaFWoOhi8ZJYv1lVbYF0R8W9gQfQWJj5K6eK."; // valid bcrypt

            String storedHash = null;
            Integer userId = null;

            // Load driver if necessary (modern JDBC may auto-load)
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
            } catch (ClassNotFoundException e) {
                log("JDBC driver not available", e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                return;
            }

            String sql = "SELECT id, password_hash FROM users WHERE username = ? LIMIT 1";

            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, username);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        userId = rs.getInt("id");
                        storedHash = rs.getString("password_hash");
                    } else {
                        // user not found — use dummy hash to ensure password verification time is similar
                        storedHash = DUMMY_BCRYPT_HASH;
                    }
                }

                // Verify password using BCrypt (constant-time)
                boolean passwordMatches = false;
                try {
                    passwordMatches = BCrypt.checkpw(password, storedHash);
                } catch (IllegalArgumentException iae) {
                    // If storedHash is corrupt/unexpected, treat as mismatch
                    log("Password hash verification failed for username: " + (userId != null ? userId : "unknown"), iae);
                    passwordMatches = false;
                }

                if (passwordMatches && userId != null) {
                    // Successful authentication: create session and set minimal attributes
                    HttpSession session = req.getSession();
                    session.setAttribute("userId", userId);
                    session.setAttribute("username", username);
                    session.setMaxInactiveInterval(15 * 60); // 15 minutes

                    // Do not expose any detailed information to client
                    resp.sendRedirect("home.jsp");
                } else {
                    // Generic unauthorized response — do not reveal whether username or password was incorrect
                    resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid username or password.");
                }

            } catch (SQLException e) {
                // Log details server-side only
                log("Database error during login", e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        }
    }
}
// ...existing code...
