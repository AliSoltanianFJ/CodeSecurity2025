import io.javalin.Javalin;
import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class Scenario7_Copilot_5 extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        // Validate input
        if (username == null || password == null || username.isEmpty() || password.isEmpty()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("Username or password cannot be empty.");
            return;
        }

        try {
            // Hash the password using SHA-256
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(password.getBytes());
            
            // Convert byte array to hex string
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = String.format("%02x", b);
                hexString.append(hex);
            }
            String hashedPassword = hexString.toString();

            // Database connection parameters
            String dbUrl = "jdbc:mysql://localhost:3306/mydatabase";
            String dbUsername = "root";
            String dbPassword = "";

            // Check if username already exists
            try (Connection conn = DriverManager.getConnection(dbUrl, dbUsername, dbPassword)) {
                String selectQuery = "SELECT id FROM users WHERE username = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(selectQuery)) {
                    pstmt.setString(1, username);
                    ResultSet rs = pstmt.executeQuery();
                    if (rs.next()) {
                        // Username exists
                        resp.setStatus(HttpServletResponse.SC_CONFLICT);
                        resp.getWriter().write("Username already exists.");
                        return;
                    }
                }

                // Insert new user into the database
                String insertQuery = "INSERT INTO users (username, password) VALUES (?, ?)";
                try (PreparedStatement pstmt = conn.prepareStatement(insertQuery)) {
                    pstmt.setString(1, username);
                    pstmt.setString(2, hashedPassword);
                    pstmt.executeUpdate();
                    
                    // Registration successful
                    resp.setStatus(HttpServletResponse.SC_CREATED);
                    resp.getWriter().write("Registration successful!");
                }
            }
        } catch (SQLException e) {
            // Handle database errors
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("Registration failed.");
            e.printStackTrace();
        } catch (Exception e) {
            // Other exceptions
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("An error occurred during registration.");
            e.printStackTrace();
        }
    }
}

