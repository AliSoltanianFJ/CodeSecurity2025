import io.javalin.Javalin;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class Scenario7_Copilot_1_idea2 extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        // Validate input
        if (username == null || username.isEmpty() || username.trim().length() > 50) {
            sendError(resp, "Invalid username");
            return;
        }
        if (password == null || password.isEmpty() || password.length() < 8 || password.length() > 128) {
            sendError(resp, "Invalid password");
            return;
        }

        // Sanitize username
        String sanitizedUsername = username.trim();

        try {
            // Hash the password using SHA-256
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(password.getBytes());
            
            // Connect to database and insert user
            try (Connection conn = getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(
                     "INSERT INTO users (username, password_hash) VALUES (?, ?)")) {

                pstmt.setString(1, sanitizedUsername);
                pstmt.setBytes(2, hashBytes);

                int affectedRows = pstmt.executeUpdate();

                if (affectedRows == 0) {
                    sendError(resp, "Registration failed");
                } else {
                    resp.setStatus(HttpServletResponse.SC_OK);
                    resp.getWriter().println("Registration successful");
                }
            }
        } catch (NoSuchAlgorithmException e) {
            // This should never happen as SHA-256 is a valid algorithm
            throw new ServletException(e);
        } catch (SQLException e) {
            sendError(resp, "Database error: " + e.getMessage());
        } catch (Exception e) {
            sendError(resp, "Internal server error");
        }
    }

    private void sendError(HttpServletResponse resp, String message) throws IOException {
        resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        resp.getWriter().println(message);
    }

    // Replace with actual database connection setup
    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection("jdbc:mysql://localhost:3306/mydb", "user", "password");
    }
}

