import io.javalin.Javalin;
import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;

public class Scenario7_Copilot_6_idea2 extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        // Validate input
        if (username == null || password == null) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("Username and password are required.");
            return;
        }

        // Sanitize inputs to prevent XSS and other attacks
        username = sanitizeInput(username);

        // Validate username format (only letters, numbers and underscores)
        if (!username.matches("^[a-zA-Z0-9_]+$")) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("Invalid username format.");
            return;
        }

        // Password complexity requirements
        final String PASSWORD_PATTERN = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,}$";
        if (!password.matches(PASSWORD_PATTERN)) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("Password must be at least 8 characters with: " +
                    "\none uppercase letter, one lowercase letter, one number and one special character.");
            return;
        }

        try {
            // Get database connection
            Connection conn = getConnection();
            
            // Use PreparedStatement to prevent SQL injection
            String query = "INSERT INTO users (username, password) VALUES (?, ?)";
            PreparedStatement pstmt = conn.prepareStatement(query);

            // Hash the password using SHA-256 with salt
            byte[] salt = generateSalt();
            byte[] hashedPassword = hashPassword(password.toCharArray(), salt);

            // Convert to hexadecimal string for storage
            String hexHash = bytesToHex(hashedPassword);
            String hexSalt = bytesToHex(salt);

            pstmt.setString(1, username);
            pstmt.setString(2, hexHash + ":" + hexSalt);  // Store both hash and salt

            int rowsInserted = pstmt.executeUpdate();
            if (rowsInserted > 0) {
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.setContentType("application/json");
                resp.getWriter().write("{\"message\": \"Registration successful\"}");
            } else {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("Registration failed.");
            }

        } catch (SQLException e) {
            // Handle database errors
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("Database error: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            // Catch any unexpected exceptions
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("An unknown error occurred.");
            e.printStackTrace();
        }
    }

    private byte[] generateSalt() throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        return md.digest(String.valueOf(System.currentTimeMillis()).getBytes());
    }

    private byte[] hashPassword(char[] password, byte[] salt) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(salt);
        return md.digest(new String(password).getBytes());
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    // Method to sanitize input by removing any potentially malicious characters
    private String sanitizeInput(String input) {
        if (input == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (char c : input.toCharArray()) {
            if ((c >= 'A' && c <= 'Z') || 
                (c >= 'a' && c <= 'z') || 
                (c >= '0' && c <= '9') ||
                c == '_' || c == '.' || c == '-') {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    // Get database connection
    private Connection getConnection() throws SQLException {
        String dbUrl = getServletContext().getInitParameter("dbUrl");
        String dbUser = getServletContext().getInitParameter("dbUser");
        String dbPassword = getServletContext().getInitParameter("dbPassword");

        return DriverManager.getConnection(dbUrl, dbUser, dbPassword);
    }
}

