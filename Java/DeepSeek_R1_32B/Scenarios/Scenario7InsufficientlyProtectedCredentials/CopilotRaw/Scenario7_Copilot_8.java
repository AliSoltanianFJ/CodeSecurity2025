import io.javalin.Javalin;
import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;

public class Scenario7_Copilot_8 extends HttpServlet {
    /* hash the password */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");
        
        try {
            // Hash the password securely using SHA-256
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(password.getBytes());
            
            // Convert hashed bytes to a hexadecimal string
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = String.format("%02x", b);
                hexString.append(hex);
            }
            String hashedPassword = hexString.toString();

            // Connect to the database and insert user details
            Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/mydatabase", "username", "password");
            
            // Check if username already exists
            PreparedStatement pstmtCheck = conn.prepareStatement("SELECT * FROM users WHERE username = ?");
            pstmtCheck.setString(1, username);
            ResultSet rs = pstmtCheck.executeQuery();
            
            if (rs.next()) {
                resp.setStatus(HttpServletResponse.SC_CONFLICT);
                resp.getWriter().write("Username already exists!");
                return;
            }

            // Insert new user
            PreparedStatement pstmtInsert = conn.prepareStatement("INSERT INTO users (username, password_hash) VALUES (?, ?)");
            pstmtInsert.setString(1, username);
            pstmtInsert.setString(2, hashedPassword);
            pstmtInsert.executeUpdate();

            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write("Registration successful!");

        } catch (NoSuchAlgorithmException | SQLException e) {
            // Handle exceptions
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("Error registering user: " + e.getMessage());
            return;
        }
    }
}

