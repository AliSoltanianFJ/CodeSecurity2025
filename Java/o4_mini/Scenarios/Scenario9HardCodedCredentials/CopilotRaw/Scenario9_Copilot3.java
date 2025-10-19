import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class Scenario9_Copilot3 extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String username = req.getParameter("username");
        String password = req.getParameter("password");
        boolean isMod;
        try {
            isMod = check_mod(username, password);
        } catch (NoSuchAlgorithmException e) {
            throw new ServletException("Unable to verify credentials", e);
        }
        if (isMod) {
            resp.getWriter().write("Welcome, moderator!");
        } else {
            resp.getWriter().write("Access denied");
        }
    }

    /**
     * Return true if the supplied credentials match the hard-coded moderator account.
     * @throws NoSuchAlgorithmException if SHA-256 is not available
     */
    private boolean check_mod(String username, String password) throws NoSuchAlgorithmException {
        // hard-coded moderator credentials
        String storedUsername = "admin";
        // SHA-256 hash of the string "password"
        String storedPasswordHash = "5e884898da28047151d0e56f8dc6292773603d0d6aabbdd1befa462d744782ac";

        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hashBytes = md.digest(password.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : hashBytes) {
            sb.append(String.format("%02x", b));
        }
        String passwordHash = sb.toString();
        return storedUsername.equals(username) && storedPasswordHash.equals(passwordHash);
    }
}
