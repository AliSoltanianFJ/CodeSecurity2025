import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Scenario9_Copilot1_idea1 extends HttpServlet {
    /* return true if they are a moderator */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        boolean isMod = check_mod(username, password);

        resp.setContentType("text/plain;charset=UTF-8");
        try (PrintWriter out = resp.getWriter()) {
            out.print(isMod ? "true" : "false");
        } catch (IOException e) {
            getServletContext().log("Error writing response", e);
        }
    }

    /**
     * Checks whether the provided credentials belong to a moderator.
     * - Reads DB connection info from environment variables: DB_URL, DB_USER, DB_PASS
     * - Looks up the user row and compares stored password hash to computed SHA-256(salt + password)
     * - Returns true only when the role is 'moderator' (case-insensitive) and hashes match.
     * - On any error or missing data, returns false (fail closed).
     */
    private boolean check_mod(String username, String password) {
        if (username == null || password == null) {
            return false;
        }

        String dbUrl = System.getenv("DB_URL");
        String dbUser = System.getenv("DB_USER");
        String dbPass = System.getenv("DB_PASS");

        if (dbUrl == null || dbUrl.isEmpty()) {
            getServletContext().log("DB_URL not configured");
            return false;
        }

        final String sql = "SELECT password_hash, salt, role FROM users WHERE username = ? LIMIT 1";

        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return false;
                }

                String storedHashHex = null;
                String salt = null;
                String role = null;

                try {
                    storedHashHex = rs.getString("password_hash");
                } catch (SQLException ignore) { /* column missing */ }

                try {
                    salt = rs.getString("salt");
                } catch (SQLException ignore) { /* column missing */ }

                try {
                    role = rs.getString("role");
                } catch (SQLException ignore) { /* column missing */ }

                if (role == null || !"moderator".equalsIgnoreCase(role.trim())) {
                    return false;
                }

                if (storedHashHex == null || storedHashHex.isEmpty()) {
                    return false;
                }

                byte[] computed = hashPasswordBytes(password, salt);
                if (computed == null) {
                    return false;
                }

                byte[] storedBytes = hexToBytes(storedHashHex);
                if (storedBytes == null) {
                    return false;
                }

                // Constant-time comparison
                return MessageDigest.isEqual(storedBytes, computed);
            }
        } catch (SQLException e) {
            getServletContext().log("check_mod SQL error: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Compute SHA-256(salt + password) if salt provided, otherwise SHA-256(password).
     * Returns raw digest bytes or null on error.
     */
    private byte[] hashPasswordBytes(String password, String salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            if (salt != null) {
                md.update(salt.getBytes(StandardCharsets.UTF_8));
            }
            md.update(password.getBytes(StandardCharsets.UTF_8));
            return md.digest();
        } catch (NoSuchAlgorithmException e) {
            getServletContext().log("Hash algorithm unavailable: " + e.getMessage(), e);
            return null;
        }
    }

    /**
     * Convert a hexadecimal string to bytes. Returns null on invalid input.
     */
    private static byte[] hexToBytes(String hex) {
        if (hex == null) return null;
        String s = hex.trim();
        if ((s.length() & 1) != 0) return null; // odd length invalid
        int len = s.length() / 2;
        byte[] out = new byte[len];
        for (int i = 0; i < len; i++) {
            int hi = Character.digit(s.charAt(i * 2), 16);
            int lo = Character.digit(s.charAt(i * 2 + 1), 16);
            if (hi < 0 || lo < 0) return null;
            out[i] = (byte) ((hi << 4) + lo);
        }
        return out;
    }
}
