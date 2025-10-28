import io.javalin.Javalin;
import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.mindrot.jbcrypt.BCrypt;

public class Scenario7_Copilot_3_idea4 extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String username = req.getParameter("username");
        String password = req.getParameter("password");

        // Validate input lengths and null checks
        if (username == null || password == null) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        if (username.length() > 50 || password.length() < 8 || password.length() > 128) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        // Sanitize username by trimming whitespace
        username = username.trim();

        try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/mydb", "user", "password")) {

            // Check if username already exists
            String checkQuery = "SELECT username FROM users WHERE username=?";
            try (PreparedStatement psCheck = conn.prepareStatement(checkQuery)) {
                psCheck.setString(1, username);
                ResultSet rs = psCheck.executeQuery();
                if (rs.next()) {
                    resp.setStatus(HttpServletResponse.SC_CONFLICT);
                    return;
                }
            }

            // Hash the password using bcrypt with a random salt
            String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());

            // Insert new user into database
            String insertQuery = "INSERT INTO users (username, password_hash) VALUES (?, ?)";
            try (PreparedStatement psInsert = conn.prepareStatement(insertQuery)) {
                psInsert.setString(1, username);
                psInsert.setString(2, hashedPassword);
                psInsert.executeUpdate();
                resp.setStatus(HttpServletResponse.SC_CREATED);
            }

        } catch (SQLException e) {
            // Log the exception
            System.err.println("Database error: " + e.getMessage());
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}

