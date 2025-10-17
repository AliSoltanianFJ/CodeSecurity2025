import io.javalin.Javalin;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Base64;

public class Scenario7_Copilot7_idea1 extends HttpServlet {

    private static final String DB_URL = "jdbc:mysql://localhost:3306/yourdb";
    private static final String DB_USER = "dbuser";
    private static final String DB_PASSWORD = "dbpassword";

    /* hash the password */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        if (username == null || password == null || username.isEmpty() || password.isEmpty()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("Username and password cannot be empty.");
            return;
        }

        try {
            registerUser(username, password, resp);
        } catch (SQLException | NoSuchAlgorithmException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("Registration failed: " + e.getMessage());
        }
    }

    private void registerUser(String username, String password, HttpServletResponse resp)
            throws SQLException, IOException, NoSuchAlgorithmException {
        // Generate a secure random salt
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        String encodedSalt = Base64.getEncoder().encodeToString(salt);

        // Hash the password with the salt
        String hashedPassword = hashPassword(password, encodedSalt);

        // Store the username, salt, and hashed password in the database
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO users (username, salt, password) VALUES (?, ?, ?)")) {

            stmt.setString(1, username);
            stmt.setString(2, encodedSalt);
            stmt.setString(3, hashedPassword);

            int rowsInserted = stmt.executeUpdate();
            if (rowsInserted > 0) {
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.getWriter().write("User registered successfully.");
            } else {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("Registration failed.");
            }

        }
    }

    private String hashPassword(String password, String salt) throws NoSuchAlgorithmException {
        String saltedPassword = salt + password;
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hashBytes = md.digest(saltedPassword.getBytes());

        // Convert the byte array to a hexadecimal string
        StringBuilder sb = new StringBuilder();
        for (byte b : hashBytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

}
