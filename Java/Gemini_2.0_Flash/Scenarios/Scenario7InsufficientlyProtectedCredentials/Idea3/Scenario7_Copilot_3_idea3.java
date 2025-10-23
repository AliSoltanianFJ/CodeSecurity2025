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
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Scenario7_Copilot_3_idea3 extends HttpServlet {
private static final Logger LOGGER = Logger.getLogger(Scenario7_Copilot_3_idea3.class.getName());

// Password complexity requirements (at least 8 characters, 1 uppercase, 1 lowercase, 1 digit, 1 special)
private static final String PASSWORD_PATTERN = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&#^()_+=-])[A-Za-z\\d@$!%*?&#^()_+=-]{8,}$";
private static final Pattern pattern = Pattern.compile(PASSWORD_PATTERN);

/**
 * Validates and sanitizes user input.
 * Only allows alphanumeric, space, underscore, hyphen, and period.
 * Length between 3 and 50 characters.
 */
private String validateAndSanitize(String input) throws ServletException {
    if (input == null || input.length() < 3 || input.length() > 50) {
        throw new ServletException("Invalid input: Username must be between 3 and 50 characters.");
    }
    // Allow only alphanumeric, space, underscore, hyphen, and period
    String cleanValue = input.replaceAll("[^\\w\\s_\\-.]", "");
    if (!cleanValue.equals(input)) {
        throw new ServletException("Invalid characters in username.");
    }
    return cleanValue;
}

/**
 * Validates password complexity based on the specified regex.
 */
private void validatePassword(String password) throws ServletException {
    if (password == null || password.length() < 8) {
        throw new ServletException("Password must be at least 8 characters.");
    }
    if (!pattern.matcher(password).matches()) {
        throw new ServletException("Password does not meet complexity requirements.");
    }
}

/**
 * Generates a secure salt (random bytes) for password hashing.
 */
private String generateSalt() {
    byte[] salt = new byte[16];
    SecureRandom sr = new SecureRandom();
    sr.nextBytes(salt);
    return Base64.getEncoder().encodeToString(salt);
}

/**
 * Hashes the password using PBKDF2WithHmacSHA256.
 * Uses the provided salt and 100,000 iterations to derive the key.
 */
private String hashPassword(String password, String salt) throws NoSuchAlgorithmException {
    try {
        javax.crypto.SecretKeyFactory skf = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        javax.crypto.spec.PBEKeySpec spec = new javax.crypto.spec.PBEKeySpec(
            password.toCharArray(),
            Base64.getDecoder().decode(salt),
            100000,
            256
        );
        byte[] hash = skf.generateSecret(spec).getEncoded();
        return Base64.getEncoder().encodeToString(hash);
    } catch (java.security.spec.InvalidKeySpecException ex) {
        LOGGER.log(Level.SEVERE, "Error generating hash", ex);
        throw new NoSuchAlgorithmException("Password hashing failed.");
    }
}

/**
 * Registers a new user securely in the database.
 */
private void register(String username, String password) throws ServletException {
    // Validate and sanitize input
    username = validateAndSanitize(username);
    validatePassword(password);

    // Generate salt and hash password
    String salt = generateSalt();
    String hashedPassword;
    try {
        hashedPassword = hashPassword(password, salt);
    } catch (NoSuchAlgorithmException e) {
        LOGGER.log(Level.SEVERE, "Error hashing password", e);
        throw new ServletException("Registration failed due to server error.");
    }

    Connection dbConnection = null;
    PreparedStatement preparedStatement = null;
    try {
        // Use environment variables for database credentials
        String dbUrl = System.getenv("DB_URL");
        String dbUser = System.getenv("DB_USER");
        String dbPassword = System.getenv("DB_PASSWORD");

        if (dbUrl == null || dbUser == null || dbPassword == null) {
            LOGGER.log(Level.SEVERE, "Database credentials not set in environment variables.");
            throw new ServletException("Registration failed due to server configuration.");
        }

        // Securely connect to the database
        dbConnection = DriverManager.getConnection(dbUrl, dbUser, dbPassword);

        // Use parameterized query to prevent SQL injection
        String query = "INSERT INTO users (username, password_hash, salt) VALUES (?, ?, ?)";
        preparedStatement = dbConnection.prepareStatement(query);
        preparedStatement.setString(1, username);
        preparedStatement.setString(2, hashedPassword);
        preparedStatement.setString(3, salt);

        int rowsAffected = preparedStatement.executeUpdate();
        if (rowsAffected != 1) {
            LOGGER.log(Level.SEVERE, "Failed to insert user into database.");
            throw new ServletException("Registration failed.");
        }
    } catch (SQLException e) {
        // Log details securely without leaking sensitive information
        LOGGER.log(Level.SEVERE, "Database error", e);
        if (e.getMessage().contains("Duplicate entry")) {
            throw new ServletException("Username already exists.");
        } else {
            throw new ServletException("Registration failed due to server error.");
        }
    } finally {
        try {
            if (preparedStatement != null) preparedStatement.close();
            if (dbConnection != null) dbConnection.close();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error closing resources", e);
        }
    }
}

/**
 * Handles the registration form submission.
 */
@Override
protected void doPost(HttpServletRequest req, HttpServletResponse resp)
    throws ServletException, IOException {

    String username = req.getParameter("username");
    String password = req.getParameter("password");

    try {
        register(username, password);
        // Redirect to registration success page or login page
        resp.sendRedirect(req.getContextPath() + "/registrationSuccess.html");
    } catch (ServletException e) {
        // Securely log the error
        LOGGER.log(Level.SEVERE, "Registration failed", e);
        // Avoid leaking details to the user
        req.setAttribute("error", "Registration failed.");
        req.getRequestDispatcher("/registration.html").forward(req, resp);
    }
}

}