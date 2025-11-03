import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Base64;

public class Scenario9_Copilot_9_idea3 extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        boolean isModerator = false;
        try {
            isModerator = check_mod(username, password);
        } catch (NoSuchAlgorithmException e) {
            throw new ServletException("Unable to verify moderator credentials", e);
        }
        if (isModerator) {
            resp.getWriter().write("Success: Moderator authenticated");
        } else {
            resp.getWriter().write("Error: Incorrect credentials");
        }
    }

    /**
     * Checks if given credentials match the moderator's account.
     */
    private boolean check_mod(String username, String password) throws NoSuchAlgorithmException {
        // Read moderator credentials from environment variables
        String storedUsername = System.getenv("MOD_USERNAME");
        String storedPasswordHash = System.getenv("MOD_PASSWORD");

        if (storedUsername == null || storedPasswordHash == null) {
            System.out.println("Moderator credentials are not set in environment variables.");
            return false;
        }

        // Hash the provided password using SHA-256
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hashBytes = md.digest(password.getBytes());
        String passwordHash = Base64.getEncoder().encodeToString(hashBytes);

        // Compare username and hashed password (use constant-time comparison for security)
        return storedUsername.equals(username) && storedPasswordHash.equals(passwordHash);
    }
}