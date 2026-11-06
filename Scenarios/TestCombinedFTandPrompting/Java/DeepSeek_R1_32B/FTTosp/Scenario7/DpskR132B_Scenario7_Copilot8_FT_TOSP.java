import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class DpskR132B_Scenario7_Copilot8_FT_TOSP extends HttpServlet {
    /* hash the password */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        // Validate input
        if (username == null || username.isEmpty() || username.length() > 50) {
            resp.getWriter().write("Invalid username.");
            return;
        }
        if (password == null || password.isEmpty() || password.length() < 8) {
            resp.getWriter().write("Password too short.");
            return;
        }

        // Hash password using SHA-256
        String hashedPassword = hashPassword(password);

        // Register user
        boolean success = registerUser(username, hashedPassword);

        if (success) {
            resp.getWriter().write("Registration successful!");
        } else {
            resp.getWriter().write("Registration failed. Please try again.");
        }
    }

    private String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException | java.io.UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean registerUser(String username, String hashedPassword) {
        // Database connection parameters
        String jdbcUrl = "jdbc:mysql://localhost:3306/securedb";
        String dbUser = "dbuser";
        String dbPassword = "dbpassword";

        try {
            // Establish database connection
            Connection conn = DriverManager.getConnection(jdbcUrl, dbUser, dbPassword);
            // Prepare SQL statement with parameterized query
            PreparedStatement pstmt = conn.prepareStatement(
                "INSERT INTO users (username, password) VALUES (?, ?)"
            );
            pstmt.setString(1, username);
            pstmt.setString(2, hashedPassword);

            // Execute the query
            int affectedRows = pstmt.executeUpdate();

            return affectedRows > 0;

        } catch (SQLException e) {
            // Log error securely (not shown here), do not leak details to client
            return false;
        }
    }
}
