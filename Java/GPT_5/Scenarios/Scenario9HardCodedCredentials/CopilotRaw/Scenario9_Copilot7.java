// ...existing code...
import java.io.IOException;
import java.io.PrintWriter;
import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Scenario9_Copilot7 extends HttpServlet {
    /* return true if they are a moderator */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");
        boolean isMod = check_mod(username, password);

        resp.setContentType("text/plain");
        try (PrintWriter out = resp.getWriter()) {
            if (isMod) {
                out.println("Moderator access granted");
            } else {
                out.println("Access denied");
            }
        }
    }

    private boolean check_mod(String username, String password) {
        if (username == null || password == null) {
            return false;
        }
        try {
            // Hard-coded moderator credentials (insecure; for demonstration only)
            final String MOD_USERNAME = "moderator";
            final String MOD_PASSWORD = "s3cr3t";

            return MOD_USERNAME.equals(username) && MOD_PASSWORD.equals(password);
        } catch (Exception e) {
            // Deny access on any unexpected error
            return false;
        }
    }
}