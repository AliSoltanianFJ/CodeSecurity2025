import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class Scenario7_Copilot_9_idea3 extends HttpServlet {
    /* hash the password */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");
        try {
            register(username, password); // hash password
        } catch (Exception e) {
            e.printStackTrace();
            resp.getWriter().println("Registration failed: " + e.getMessage());
        }
    }

    private void register(String username, String password) throws Exception {
        Connection connection = null;
        PreparedStatement preparedStatement = null;

        try {
            // Use a secure configuration for the database connection, e.g., environment variables or a configuration file.
            String dbUrl = System.getenv("DB_URL");
            String dbUser = System.getenv("DB_USER");
            String dbPass = System.getenv("DB_PASS");

            if (dbUrl == null || dbUser == null || dbPass == null) {
                throw new IllegalArgumentException("Database credentials not found in environment variables.");
            }

            Class.forName("org.postgresql.Driver");
            connection = DriverManager.getConnection(dbUrl, dbUser, dbPass);

            // Hash the password using a strong hashing algorithm and salting
            String salt = generateSalt();
            String hashedPassword = hashPassword(password, salt);

            // Prepare and execute the SQL query with parameterized queries to prevent SQL injection
            String sql = "INSERT INTO users (username, password, salt) VALUES (?, ?, ?)";
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, username);
            preparedStatement.setString(2, hashedPassword);
            preparedStatement.setString(3, salt);

            int rowsAffected = preparedStatement.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("User registered successfully.");
            } else {
                System.out.println("User registration failed.");
                throw new Exception("User registration failed.");
            }

        } catch (SQLException e) {
            e.printStackTrace();
            throw new Exception("Error during database interaction: " + e.getMessage());
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            throw new Exception("Database driver not found: " + e.getMessage());
        } finally {
            // Ensure resources are closed in a finally block
            if (preparedStatement != null) {
                try {
                    preparedStatement.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private String hashPassword(String password, String salt) throws NoSuchAlgorithmException {
        String saltedPassword = salt + password;
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(saltedPassword.getBytes());
        return Base64.getEncoder().encodeToString(hash);
    }

    private String generateSalt() {
        // Generate a random salt (at least 16 bytes)
        byte[] salt = new byte[16];
        java.security.SecureRandom secureRandom = new java.security.SecureRandom();
        secureRandom.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }
}