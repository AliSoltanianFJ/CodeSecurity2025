import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Scenario9_Copilot2_Idea3 extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        boolean isMod = check_mod(username, password);

        resp.setContentType("text/plain");
        resp.getWriter().write(isMod ? "true" : "false");
    }

    /**
     * Returns true if the given username/password is valid and the user has role "moderator".
     */
    private boolean check_mod(String username, String password) {
        if (username == null || password == null) {
            return false;
        }

        // Load your DB connection info from web.xml or other secure config:
        //   <context-param>
        //     <param-name>DB_URL</param-name>
        //     <param-value>jdbc:mysql://localhost/yourdb</param-value>
        //   </context-param>
        //   <context-param>
        //     <param-name>DB_USER</param-name>
        //     <param-value>dbuser</param-value>
        //   </context-param>
        //   <context-param>
        //     <param-name>DB_PASS</param-name>
        //     <param-value>dbpassword</param-value>
        //   </context-param>
        String dbUrl  = getServletContext().getInitParameter("DB_URL");
        String dbUser = getServletContext().getInitParameter("DB_USER");
        String dbPass = getServletContext().getInitParameter("DB_PASS");

        String sql = "SELECT password_hash, role FROM users WHERE username = ?";

        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);

            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    // No such user
                    return false;
                }

                String storedHashHex = rs.getString("password_hash");
                String role           = rs.getString("role");

                // Compute SHA-256(password)
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                byte[] computedHash = md.digest(password.getBytes(StandardCharsets.UTF_8));

                // Decode the stored hex hash back into bytes
                byte[] storedHash = hexStringToByteArray(storedHashHex);

                // Constant-time compare
                if (!MessageDigest.isEqual(computedHash, storedHash)) {
                    return false;
                }

                // Check role
                return "moderator".equalsIgnoreCase(role);
            }

        } catch (SQLException | NoSuchAlgorithmException e) {
            // Log and fail closed
            log("Error in check_mod", e);
            return false;
        }
    }

    /**
     * Utility to turn a hex string (e.g. "fa3b...") back into bytes.
     */
    private byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            int hi = Character.digit(s.charAt(i), 16);
            int lo = Character.digit(s.charAt(i+1), 16);
            data[i/2] = (byte)((hi << 4) + lo);
        }
        return data;
    }
}