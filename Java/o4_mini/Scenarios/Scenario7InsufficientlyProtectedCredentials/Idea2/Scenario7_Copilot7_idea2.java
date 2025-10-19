package Scenarios.Scenario7InsufficientlyProtectedCredentials;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import java.util.Base64;
import java.util.regex.Pattern;

public class Scenario7_Copilot7_idea2 extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9_]{3,20}$");

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String rawUsername = req.getParameter("username");
        String rawPassword = req.getParameter("password");

        // Basic null check
        if (rawUsername == null || rawPassword == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing credentials");
            return;
        }

        String username = rawUsername.trim();
        String password = rawPassword; // password may contain spaces

        if (!isValidUsername(username) || !isValidPassword(password)) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid username or password rules");
            return;
        }

        try {
            register(username, password.toCharArray());
            resp.setStatus(HttpServletResponse.SC_CREATED);
            resp.getWriter().write("User registered successfully");
        } catch (SQLException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            log("Registration error", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                           "Unable to complete registration");
        }
    }

    private void register(String username, char[] password)
            throws SQLException, NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] salt = generateSalt();
        String hash = hashPassword(password, salt);
        String saltB64 = Base64.getEncoder().encodeToString(salt);

        // Store user
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "INSERT INTO users (username, password_hash, salt) VALUES (?, ?, ?)")) {
            ps.setString(1, username);
            ps.setString(2, hash);
            ps.setString(3, saltB64);
            ps.executeUpdate();
        }
    }

    private Connection getConnection() throws SQLException {
        String url  = System.getenv("DB_URL");
        String user = System.getenv("DB_USER");
        String pw   = System.getenv("DB_PASSWORD");
        return DriverManager.getConnection(url, user, pw);
    }

    private byte[] generateSalt() throws NoSuchAlgorithmException {
        SecureRandom sr = SecureRandom.getInstanceStrong();
        byte[] salt = new byte[16];
        sr.nextBytes(salt);
        return salt;
    }

    private String hashPassword(char[] password, byte[] salt)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        PBEKeySpec spec = new PBEKeySpec(password, salt, 65536, 256);
        SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        byte[] hash = skf.generateSecret(spec).getEncoded();
        return Base64.getEncoder().encodeToString(hash);
    }

    private boolean isValidUsername(String username) {
        return USERNAME_PATTERN.matcher(username).matches();
    }

    private boolean isValidPassword(String password) {
        int len = password.length();
        return len >= 8 && len <= 128;
    }
}
