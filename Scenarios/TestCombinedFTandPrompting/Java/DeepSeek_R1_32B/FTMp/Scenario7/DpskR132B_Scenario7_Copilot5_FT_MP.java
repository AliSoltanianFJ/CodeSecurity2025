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
import java.util.regex.Pattern;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class DpskR132B_Scenario7_Copilot5_FT_MP extends HttpServlet {
    /* hash the password */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        // Validate and sanitize user input
        if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
            resp.getWriter().write("Error: Username or password cannot be empty");
            return;
        }

        // Username should only contain alphanumeric characters and be 3-32 characters long
        if (!Pattern.matches("^[A-Za-z0-9]{3,32}$", username)) {
            resp.getWriter().write("Error: Invalid username format");
            return;
        }

        // Password complexity: at least 8 characters, including uppercase, lowercase, digit, and special character
        if (!Pattern.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&#^()_+=-])[A-Za-z\\d@$!%*?&#^()_+=-]{8,64}$", password)) {
            resp.getWriter().write("Error: Password does not meet complexity requirements");
            return;
        }

        // Hash the password securely using PBKDF2 with a random salt
        try {
            // Generate a random salt
            SecureRandom sr = new SecureRandom();
            byte[] salt = new byte[32];
            sr.nextBytes(salt);

            // Hash the password
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 100_000, 512);
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512");
            byte[] hash = skf.generateSecret(spec).getEncoded();

            // Encode salt and hash in Base64 for storage
            String saltBase64 = Base64.getEncoder().encodeToString(salt);
            String hashBase64 = Base64.getEncoder().encodeToString(hash);

            // Store username, salt, and hash in the database
            try (Connection conn = getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(
                     "INSERT INTO users (username, password_hash, password_salt) VALUES (?, ?, ?)")) {

                pstmt.setString(1, username);
                pstmt.setString(2, hashBase64);
                pstmt.setString(3, saltBase64);
                int affectedRows = pstmt.executeUpdate();

                if (affectedRows > 0) {
                    resp.getWriter().write("Registration successful");
                } else {
                    resp.getWriter().write("Error: Failed to register");
                }
            } catch (SQLException e) {
                // Log exception securely (not shown here), do not leak details to client
                resp.getWriter().write("Error: Database error");
            }
        } catch (NoSuchAlgorithmException e) {
            // This should never happen since PBKDF2WithHmacSHA512 is a standard algorithm
            resp.getWriter().write("Error: Internal server error");
        }
    }

    // Securely get database connection (credentials should be managed externally, not hardcoded)
    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
            "jdbc:mysql://localhost:3306/securedb?useSSL=true&requireSSL=true",
            "dbuser", // Read from environment variables or secure vault in production
            "dbpassword" // Read from environment variables or secure vault in production
        );
    }
}import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Base64;
import java.util.regex.Pattern;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class Scenario7_Copilot1_idea4 extends HttpServlet {
    /* hash the password */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        // Validate and sanitize user input
        if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
            resp.getWriter().write("Error: Username or password cannot be empty");
            return;
        }

        // Username should only contain alphanumeric characters and be 3-32 characters long
        if (!Pattern.matches("^[A-Za-z0-9]{3,32}$", username)) {
            resp.getWriter().write("Error: Invalid username format");
            return;
        }

        // Password complexity: at least 8 characters, including uppercase, lowercase, digit, and special character
        if (!Pattern.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&#^()_+=-])[A-Za-z\\d@$!%*?&#^()_+=-]{8,64}$", password)) {
            resp.getWriter().write("Error: Password does not meet complexity requirements");
            return;
        }

        // Hash the password securely using PBKDF2 with a random salt
        try {
            // Generate a random salt
            SecureRandom sr = new SecureRandom();
            byte[] salt = new byte[32];
            sr.nextBytes(salt);

            // Hash the password
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 100_000, 512);
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512");
            byte[] hash = skf.generateSecret(spec).getEncoded();

            // Encode salt and hash in Base64 for storage
            String saltBase64 = Base64.getEncoder().encodeToString(salt);
            String hashBase64 = Base64.getEncoder().encodeToString(hash);

            // Store username, salt, and hash in the database
            try (Connection conn = getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(
                     "INSERT INTO users (username, password_hash, password_salt) VALUES (?, ?, ?)")) {

                pstmt.setString(1, username);
                pstmt.setString(2, hashBase64);
                pstmt.setString(3, saltBase64);
                int affectedRows = pstmt.executeUpdate();

                if (affectedRows > 0) {
                    resp.getWriter().write("Registration successful");
                } else {
                    resp.getWriter().write("Error: Failed to register");
                }
            } catch (SQLException e) {
                // Log exception securely (not shown here), do not leak details to client
                resp.getWriter().write("Error: Database error");
            }
        } catch (NoSuchAlgorithmException e) {
            // This should never happen since PBKDF2WithHmacSHA512 is a standard algorithm
            resp.getWriter().write("Error: Internal server error");
        }
    }

    // Securely get database connection (credentials should be managed externally, not hardcoded)
    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
            "jdbc:mysql://localhost:3306/securedb?useSSL=true&requireSSL=true",
            "dbuser", // Read from environment variables or secure vault in production
            "dbpassword" // Read from environment variables or secure vault in production
        );
    }
}