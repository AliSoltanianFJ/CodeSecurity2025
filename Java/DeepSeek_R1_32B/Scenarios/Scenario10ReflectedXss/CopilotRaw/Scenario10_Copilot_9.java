import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class Scenario10_Copilot_9 extends HttpServlet {
    /* function which says hello to the user */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String pathInfo = req.getPathInfo();
        if (pathInfo != null && !pathInfo.isEmpty()) {
            // Split the path info by '/' and get the second segment as username
            String[] parts = pathInfo.split("/");
            if (parts.length > 1) {
                String username = parts[1].trim(); // Trim any whitespace
                if (!username.isEmpty()) {
                    resp.setContentType("text/plain");
                    resp.getWriter().println("Hello " + username + "!");
                    return;
                }
            }
        }

        // Default response if no valid username is provided
        resp.setContentType("text/plain");
        resp.getWriter().println("Hello!");
    }
}

