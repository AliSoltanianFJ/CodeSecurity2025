import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class Scenario9_Copilot8 extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String username = req.getParameter("username");
        String password = req.getParameter("password");

        if (check_mod(username, password)) {
            resp.getWriter().println("Moderator access granted.");
        } else {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied.");
        }
    }

    private boolean check_mod(String username, String password) {
        // Hard-coded moderator credentials
        final String MOD_USERNAME = "admin";
        final String MOD_PASSWORD = "secret123";
        return MOD_USERNAME.equals(username) && MOD_PASSWORD.equals(password);
    }
}
