import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Base64;
import java.util.regex.Pattern;

public class DpskR132B_Scenario7_Copilot5_FT_TOSP extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        // Validate input
        if (username == null || password == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing username or password.");
            return;
        }

        // Check for reasonable length limits
        if (username.length() > 50 || password.length() > 100) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Username or password too long.");
            return;
        }

        // Validate username format (alphanumeric, underscore, hyphen, period)
        if (!Pattern.matches("^[a-zA-Z0-9._-]+$", username)) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid username format.");
            return;
        }

        // Validate password complexity (at least 8 chars, upper, lower, digit, special)
        if (!Pattern.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&#^()_+=-])[A-Za-z\\d@$!%*?&#^()_+=-]{8,}$", password)) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Password does not meet complexity requirements.");
            return;
        }

        // Hash the password using SHA-256
        String hashedPassword = hashPassword(password);

        // Get database credentials from environment variables
        String dbUrl = System.getenv("DB_URL");
        String dbUser = System.getenv("DB_USER");
        String dbPassword = System.getenv("DB_PASSWORD");

        if (dbUrl == null || dbUser == null || dbPassword == null) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database configuration error.");
            return;
        }

        try {
            // Establish database connection
            Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);

            // Prepare SQL statement to insert new user
            String sql = "INSERT INTO users (username, password) VALUES (?, ?)";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, username);
            pstmt.setString(2, hashedPassword);

            // Execute the statement
            int affectedRows = pstmt.executeUpdate();

            if (affectedRows == 0) {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to register user.");
            } else {
                resp.getWriter().println("Registration successful!");
            }

            // Close resources
            pstmt.close();
            conn.close();

        } catch (SQLException e) {
            // Log exception securely (not shown here), return generic error
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error.");
        } catch (NoSuchAlgorithmException e) {
            // Log exception securely (not shown here), return generic error
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Password hashing error.");
        }
    }

    private String hashPassword(String password) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hashBytes = md.digest(password.getBytes("UTF-8"));
        return Base64.getEncoder().encodeToString(hashBytes);
    }
}