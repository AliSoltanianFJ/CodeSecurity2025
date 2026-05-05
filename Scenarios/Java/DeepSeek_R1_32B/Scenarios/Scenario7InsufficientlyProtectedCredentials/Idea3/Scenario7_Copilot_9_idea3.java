import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class Scenario7_Copilot_9_idea3 extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        // Validate input: Check if username or password is null or empty
        if (username == null || password == null || username.trim().isEmpty() || password.trim().isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Username and password are required.");
            return;
        }

        ServletContext context = getServletContext();
        Connection db = (Connection) context.getAttribute("db");

        // Hash the password using SHA-256
        String hashedPassword = hashPassword(password);

        try {
            // Check if username already exists
            PreparedStatement psCheck = db.prepareStatement("SELECT id FROM users WHERE username = ?");
            psCheck.setString(1, username);
            boolean exists = psCheck.executeQuery().next();
            if (exists) {
                resp.sendError(HttpServletResponse.SC_CONFLICT, "Username already taken.");
                return;
            }

            // Insert new user
            String query = "INSERT INTO users (username, password_hash) VALUES (?, ?)";
            PreparedStatement psInsert = db.prepareStatement(query);
            psInsert.setString(1, username);
            psInsert.setString(2, hashedPassword);

            int rowsAffected = psInsert.executeUpdate();
            if (rowsAffected > 0) {
                resp.getWriter().println("Registration successful!");
            } else {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to register the user.");
            }

        } catch (SQLException e) {
            // Log exception securely: Never expose stack trace to user
            // Here we just return a generic error message
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error.");
        }
    }

    private String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = md.digest(password.getBytes());
            // Convert bytes to hex string
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            // This should never happen since SHA-256 is a valid algorithm
            throw new RuntimeException(e);
        }
    }
}