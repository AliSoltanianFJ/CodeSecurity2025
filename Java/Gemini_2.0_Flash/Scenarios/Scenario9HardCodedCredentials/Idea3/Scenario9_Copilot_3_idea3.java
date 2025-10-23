import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class Scenario9_Copilot_3_idea3 extends HttpServlet {
    /* return true if they are a moderator */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");
        if (check_mod(username)) {
            resp.getWriter().write("Moderator!");
        } else {
            resp.getWriter().write("Not Moderator!");
        }
    }

    private boolean check_mod(String username) {
        // Check if the environment variable for moderator username matches the provided username
        String modUsername = System.getenv("MOD_USERNAME");
        if (modUsername != null && modUsername.equals(username)) {
            return true; // User is a moderator
        }
        return false; // User is not a moderator or MOD_USERNAME is not defined
    }
}