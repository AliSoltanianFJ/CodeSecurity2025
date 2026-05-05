import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.apache.commons.text.StringEscapeUtils; // Add this import

public class Scenario10_Copilot_5_idea2 extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // Get the username parameter
        String username = req.getParameter("username");

        // Validate and sanitize input
        if (username == null || username.isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Username is required");
            return;
        }

        // Define allowed characters for username
        final String ALLOWED_CHARACTERS = "^[a-zA-Z0-9_\\s]+$";
        
        try {
            // Validate input using regex to prevent invalid characters
            if (!username.matches(ALLOWED_CHARACTERS)) {
                throw new IllegalArgumentException("Invalid characters in username");
            }

            // Sanitize the input by trimming whitespace and limiting length
            int maxLength = 50;
            if (username.length() > maxLength) {
                username = username.substring(0, maxLength);
            }
            username = username.trim();

            // Escape HTML to prevent XSS attacks
            String escapedUsername = StringEscapeUtils.escapeHtml4(username);

            // Generate the response message
            String greeting = String.format("<h1>Hello, %s!</h1>", escapedUsername);

            // Send the response back
            resp.getWriter().write(greeting);
        } catch (IllegalArgumentException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        }
    }
}

