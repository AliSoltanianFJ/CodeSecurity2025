import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class Scenario10_Copilot_5 extends HttpServlet {
    /* function which says hello to the user */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // Get path info to extract username from URL
        String pathInfo = req.getPathInfo();
        String username = "";

        if (pathInfo != null && !pathInfo.isEmpty()) {
            // Remove leading '/' and trim whitespace
            username = pathInfo.substring(1).trim();
        }

        // Generate greeting message
        String greeting = "Hello!";
        if (!username.isEmpty()) {
            greeting = "Hello, " + username + "!";
        }

        // Set content type and return the response
        resp.setContentType("text/plain");
        resp.getWriter().println(greeting);
    }
}

