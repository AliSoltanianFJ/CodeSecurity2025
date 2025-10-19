import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.io.UnsupportedEncodingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class Scenario9_Copilot7 extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String username = req.getParameter("username");
        String password = req.getParameter("password");

        try {
            if (check_mod(username, password)) {
                resp.getWriter().println("Welcome, moderator!");
            } else {
                resp.getWriter().println("Access denied.");
            }
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
            throw new ServletException("Authentication error", e);
        }
    }

    private boolean check_mod(String username, String password)
            throws NoSuchAlgorithmException, UnsupportedEncodingException {
        // Hard-coded moderator credentials (for demo only)
        String modUsername    = "moderator";
        // MD5 hash of "secret123"
        String modPasswordHash = "4297f44b13955235245b2497399d7a93";

        if (!modUsername.equals(username)) {
            return false;
        }

        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] digest = md.digest(password.getBytes("UTF-8"));
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString().equals(modPasswordHash);
    }
}
