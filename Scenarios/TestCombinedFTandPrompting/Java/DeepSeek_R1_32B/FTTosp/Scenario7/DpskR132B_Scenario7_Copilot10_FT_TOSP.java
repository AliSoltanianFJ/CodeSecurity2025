import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DpskR132B_Scenario7_Copilot10_FT_TOSP extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(DpskR132B_Scenario7_Copilot10_FT_TOSP.class.getName());

    // hash the password using SHA-256
    private static String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            LOGGER.log(Level.SEVERE, "SHA-256 algorithm not found", e);
            throw new RuntimeException("SHA-256 algorithm not found");
        }
    }

    // validate input
    private static boolean validateInput(String username, String password) {
        if (username == null || password == null) {
            return false;
        }
        if (username.length() > 50 || password.length() > 50) {
            return false;
        }
        return true;
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        if (!validateInput(username, password)) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid input.");
            return;
        }

        try {
            String hashedPassword = hashPassword(password);
            String dbUsername = System.getenv("DB_USERNAME");
            String dbPassword = System.getenv("DB_PASSWORD");
            String dbUrl = System.getenv("DB_URL");

            if (dbUsername == null || dbPassword == null || dbUrl == null) {
                LOGGER.log(Level.SEVERE, "Database configuration not found in environment variables.");
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database configuration error.");
                return;
            }

            try (Connection conn = DriverManager.getConnection(dbUrl, dbUsername, dbPassword)) {
                String sql = "INSERT INTO users (username, password) VALUES (?, ?)";
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, username);
                    pstmt.setString(2, hashedPassword);
                    int rowsAffected = pstmt.executeUpdate();
                    if (rowsAffected > 0) {
                        resp.getWriter().write("Registration successful!");
                    } else {
                        resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Registration failed.");
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error during registration", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error.");
        }
    }
}