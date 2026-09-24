import io.javalin.Javalin;
import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.mindrot.jbcrypt.BCrypt;

public class Scenario7_Copilot3 extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        if (username == null || password == null || username.trim().isEmpty() || password.trim().isEmpty()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("Username and password must not be empty.");
            return;
        }

        // Securely hash the password
        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());

        // Example: Use a datasource or a secure connection, not hardcoded credentials
        try (Connection conn = getDataSource().getConnection()) {
            String sql = "INSERT INTO users (username, password_hash) VALUES (?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, username);
                stmt.setString(2, hashedPassword);
                stmt.executeUpdate();
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.getWriter().write("User registered successfully.");
            }
        } catch (SQLException e) {
            // Log the stack trace for admins, but don't expose it to users
            e.printStackTrace(); // Consider using a logger
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("Internal server error.");
        }
    }

    // Example datasource accessor (inject or use JNDI in real apps)
    private javax.sql.DataSource getDataSource() {
        // Implement this to obtain your connection securely; do not hardcode!
        throw new UnsupportedOperationException("Provide a real DataSource.");
    }
}