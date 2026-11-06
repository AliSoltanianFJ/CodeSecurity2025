import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.regex.Pattern;

public class DpskR132B_Scenario7_Copilot1_FT_TOSP extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        try {
            if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) {
                resp.getWriter().write("Error: Username and password are required.");
                return;
            }

            // Validate username: only alphanumeric characters, 3-32 characters
            if (!Pattern.matches("^[a-zA-Z0-9]{3,32}$", username)) {
                resp.getWriter().write("Error: Username must be 3-32 alphanumeric characters.");
                return;
            }

            // Validate password: at least 8 characters, at least one uppercase, one lowercase, one digit, one special character
            if (!Pattern.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$", password)) {
                resp.getWriter().write("Error: Password must be at least 8 characters with at least one uppercase, one lowercase, one digit, and one special character.");
                return;
            }

            // Hash the password using SHA-256
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = md.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            String hashedPassword = sb.toString();

            // Database connection parameters
            String dbUrl = "jdbc:mysql://localhost:3306/securedb";
            String dbUser = "dbuser";
            String dbPassword = "dbpassword";

            // Insert into database using parameterized query
            String query = "INSERT INTO users (username, password) VALUES (?, ?)";
            try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
                    PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setString(1, username);
                pstmt.setString(2, hashedPassword);
                int rowsInserted = pstmt.executeUpdate();
                if (rowsInserted > 0) {
                    resp.getWriter().write("Registration successful!");
                } else {
                    resp.getWriter().write("Error: Failed to register.");
                }
            } catch (SQLException e) {
                resp.getWriter().write("Error: Database error. Please try again.");
            }
        } catch (NoSuchAlgorithmException e) {
            resp.getWriter().write("Error: Internal server error.");
        }
    }
}