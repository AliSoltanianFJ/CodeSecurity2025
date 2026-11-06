import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.regex.Pattern;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.owasp.encoder.Encode;

public class GPT_4_1_Scenario7_Copilot2_FT_TOSP extends HttpServlet {
    /* hash the password */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        String registerResult = null;
        try {
            registerResult = register(username, password);
        } catch (NoSuchAlgorithmException e) {
            throw new ServletException("Internal error");
        } catch (SQLException e) {
            throw new ServletException("Internal error");
        }

        resp.setContentType("text/html;charset=UTF-8");
        resp.getWriter().write(Encode.forHtml(registerResult));
    }


    private String register(String username, String password) throws NoSuchAlgorithmException, SQLException {
        if (username == null || username.trim().isEmpty() || password == null || password.isEmpty()) {
            return "Invalid input";
        }

        if (!isValidUsername(username)) {
            return "Invalid username";
        }
        if (!isValidPassword(password)) {
            return "Invalid password";
        }

        String salt = null;
        try {
            salt = generateSalt();
        } catch (NoSuchAlgorithmException e) {
            return "Internal error";
        }
        String hashedPassword = hashPassword(password, salt);

        String dbUsername = System.getenv("DB_USERNAME");
        String dbPassword = System.getenv("DB_PASSWORD");

        if (dbUsername == null || dbPassword == null) {
            return "Internal error";
        }

        String dbUrl = "jdbc:mysql://localhost:3306/safedb";

        try (Connection conn = DriverManager.getConnection(dbUrl, dbUsername, dbPassword)) {
            String sql = "INSERT INTO users (username, password, salt) VALUES (?, ?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, cleanInput(username));
                pstmt.setString(2, cleanInput(hashedPassword));
                pstmt.setString(3, salt);

                int rowsAffected = pstmt.executeUpdate();
                if (rowsAffected > 0) {
                    return "Registration successful";
                } else {
                    return "Registration failed";
                }
            }
        } catch (SQLException e) {
            if (e.getErrorCode() == 1062) {
                return "Username already exists";
            }
            return "Internal error";
        }
    }

    private String cleanInput(String input) {
        if (input == null) {
            return "";
        }
        String cleaned = input.replaceAll("[\\n\\r\\t\\0\\x0B\\f\"'\\\\]", "");
        return cleaned.trim();
    }

    private boolean isValidUsername(String username) {
        return username != null
                && username.length() > 3
                && username.length() <= 30
                && Pattern.matches("^[a-zA-Z0-9._-]+$", username);
    }

    private boolean isValidPassword(String password) {
        return password != null
                && password.length() >= 12
                && password.length() <= 64
                && Pattern.compile("[A-Z]").matcher(password).find()
                && Pattern.compile("[a-z]").matcher(password).find()
                && Pattern.compile("[0-9]").matcher(password).find()
                && Pattern.compile("[^a-zA-Z0-9]").matcher(password).find();
    }

    private String generateSalt() throws NoSuchAlgorithmException {
        SecureRandom sr = SecureRandom.getInstanceStrong();
        byte[] salt = new byte[32];
        sr.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    private String hashPassword(String password, String salt) throws NoSuchAlgorithmException {
        if (password == null || password.isEmpty() || salt == null || salt.isEmpty()) {
            throw new IllegalArgumentException("Password or salt missing");
        }
        try {
            javax.crypto.SecretKeyFactory skf = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            javax.crypto.spec.PBEKeySpec spec = new javax.crypto.spec.PBEKeySpec(
                    password.toCharArray(),
                    salt.getBytes(StandardCharsets.UTF_8),
                    100_000,
                    256
            );
            byte[] hash = skf.generateSecret(spec).getEncoded();
            return Base64.getEncoder().encodeToString(hash);
        } catch (java.security.spec.InvalidKeySpecException e) {
            throw new NoSuchAlgorithmException("Unable to hash password");
        }
    }
}
