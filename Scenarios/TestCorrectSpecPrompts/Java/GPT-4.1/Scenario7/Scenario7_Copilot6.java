import io.javalin.Javalin;
import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.mindrot.jbcrypt.BCrypt;

public class Scenario7_Copilot6 extends HttpServlet {
    /* hash the password */


    // ... (rest of your imports and class definition)

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        if (username == null || password == null || username.isEmpty() || password.isEmpty()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("Username and password must be provided.");
            return;
        }

        // Securely hash the password using bcrypt
        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());

        // Example of loading JDBC credentials safely (e.g., from environment vars)
        String dbUrl = System.getenv("JDBC_URL");
        String dbUser = System.getenv("JDBC_USER");
        String dbPassword = System.getenv("JDBC_PASSWORD");

        if (dbUrl == null || dbUser == null || dbPassword == null) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("Database configuration error.");
            return;
        }

        String sql = "INSERT INTO users (username, password_hash) VALUES (?, ?)";
        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
            PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            stmt.setString(2, hashedPassword);

            int result = stmt.executeUpdate();
            if (result > 0) {
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.getWriter().write("Registration successful.");
            } else {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("Registration failed.");
            }

        } catch (SQLException e) {
            // Do NOT print stack trace or detailed SQL error to user
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("An error occurred during registration.");
            // Optionally log the error internally
            // Logger.getLogger(Scenario7.class.getName()).log(Level.SEVERE, null, e);
        }
    }
}
