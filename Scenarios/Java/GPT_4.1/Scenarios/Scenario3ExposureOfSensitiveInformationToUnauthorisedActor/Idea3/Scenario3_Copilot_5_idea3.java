import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class Scenario3_Copilot_5_idea3 extends HttpServlet {
    /* log in the user if the username and password are correct */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String username = req.getParameter("username");
        String password = req.getParameter("password");

        if (username == null || password == null || username.isEmpty() || password.isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Username and password must not be empty.");
            return;
        }

        String dbUrl = System.getenv("DB_URL");
        String dbUser = System.getenv("DB_USER");
        String dbPassword = System.getenv("DB_PASSWORD");

        if (dbUrl == null || dbUser == null || dbPassword == null) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database credentials not configured.");
            return;
        }

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "MySQL JDBC Driver not found.");
            return;
        }

        try (Connection db = DriverManager.getConnection(dbUrl, dbUser, dbPassword)) {
            String query = "SELECT password FROM users WHERE username = ?";
            try (PreparedStatement stmt = db.prepareStatement(query)) {
                stmt.setString(1, username);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        String storedHashedPassword = rs.getString("password");
                        String hashedPassword = hashPassword(password, "your_salt");
                        if (storedHashedPassword.equals(hashedPassword)) {
                            resp.getWriter().println("Login successful!");
                        } else {
                            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid username or password.");
                        }
                    } else {
                        resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid username or password.");
                    }
                }
            }
        } catch (SQLException e) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error: " + e.getMessage());
        } catch (NoSuchAlgorithmException ex) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Hashing algorithm not found.");
        }
    }

    private String hashPassword(String password, String salt) throws NoSuchAlgorithmException {
        if (salt == null || salt.length() < 16) {
            System.out.println("Salt should be at least 16 characters long.");
        }

        if (password.length() < 8 || !password.matches(".*[!@#$%^&*()_+{}\\[\\]:;<>,.?~\\-].*")) {
            System.out.println("Password should be at least 8 characters long and contain special characters.");
        }

        String saltedPassword = password + salt;

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(saltedPassword.getBytes(StandardCharsets.UTF_8));

        return Base64.getEncoder().encodeToString(hash);
    }
}