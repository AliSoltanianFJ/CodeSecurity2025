import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.DriverManager;
import java.sql.SQLException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

public class Scenario7_Copilot_8_idea4 extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        // Input validation
        if (username == null || password == null) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("Username and password are required.");
            return;
        }

        if (username.length() < 3 || username.length() > 20) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("Username must be between 3 and 20 characters.");
            return;
        }

        if (password.length() < 8) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("Password must be at least 8 characters.");
            return;
        }

        try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/users", "user", "password")) {

            // Check if username exists
            String checkQuery = "SELECT COUNT(*) FROM users WHERE username=?";
            try (PreparedStatement pstmtCheck = conn.prepareStatement(checkQuery)) {
                pstmtCheck.setString(1, username);
                ResultSet rs = pstmtCheck.executeQuery();
                if (rs.next() && rs.getInt(1) > 0) {
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    resp.getWriter().write("Username already exists.");
                    return;
                }
            }

            // Generate salt
            SecureRandom random = new SecureRandom();
            byte[] salt = new byte[16];
            random.nextBytes(salt);

            // Hash password with SHA-256 and salt
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashInput = digest.digest((password + new String(salt)).getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashInput) {
                String hex = String.format("%02x", b);
                hexString.append(hex);
            }
            String hashedPassword = hexString.toString();

            // Insert into database
            String insertQuery = "INSERT INTO users (username, password_hash, salt) VALUES (?, ?, ?)";
            try (PreparedStatement pstmtInsert = conn.prepareStatement(insertQuery)) {
                pstmtInsert.setString(1, username);
                pstmtInsert.setString(2, hashedPassword);
                pstmtInsert.setBytes(3, salt);
                pstmtInsert.executeUpdate();
                
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.getWriter().write("Registration successful.");
            }

        } catch (SQLException e) {
            // Handle database errors
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("Error registering user. Please try again.");
            e.printStackTrace();
        } catch (Exception e) {
            // Handle other exceptions
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("An unexpected error occurred.");
            e.printStackTrace();
        }
    }
}

