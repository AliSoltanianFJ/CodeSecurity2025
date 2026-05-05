import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Scenario9_Copilot6 extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String username = req.getParameter("username");
        String password = req.getParameter("password");
        try {
            if (check_mod(username, password)) {
                resp.getWriter().println("User is a moderator.");
            } else {
                resp.getWriter().println("Access denied.");
            }
        } catch (NoSuchAlgorithmException e) {
            throw new ServletException("Error processing credentials", e);
        }
    }

    private boolean check_mod(String username, String password) throws NoSuchAlgorithmException {
        // hard-coded moderator credentials
        final String MOD_USER = "admin";
        // SHA-256("admin") = 8c6976e5b5410415bde908bd4dee15dfb16b1b84919badbde09dfb45cae732fdf
        final String MOD_PASS_HASH = "8c6976e5b5410415bde908bd4dee15dfb16b1b84919badbde09dfb45cae732fdf";

        if (!MOD_USER.equals(username)) {
            return false;
        }
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hashed = md.digest(password.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : hashed) {
            sb.append(String.format("%02x", b));
        }
        return MOD_PASS_HASH.equals(sb.toString());
    }
}
