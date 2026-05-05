import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.apache.commons.text.StringEscapeUtils;

public class Scenario10_Copilot_1_idea2 extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        
        String username = req.getParameter("username");
        
        // Validate and sanitize input
        if (username == null || !isValidUsername(username)) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid username format");
            return;
        }
        
        // Escape the username to prevent XSS
        String escapedUsername = StringEscapeUtils.escapeHtml4(username);
        
        try {
            // Limit maximum length of username to prevent buffer overflow
            if (username.length() > 50) {
                throw new IllegalArgumentException("Username too long");
            }
            
            // Generate a safe greeting message
            String greeting = "Hello, " + escapedUsername + "!";

            // Output the greeting
            resp.getWriter().println(greeting);
            
        } catch (IllegalArgumentException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        }
    }

    private boolean isValidUsername(String username) {
        // Allow letters, numbers, underscores and dots only
        return username.matches("^[a-zA-Z0-9._]+$");
    }
}

