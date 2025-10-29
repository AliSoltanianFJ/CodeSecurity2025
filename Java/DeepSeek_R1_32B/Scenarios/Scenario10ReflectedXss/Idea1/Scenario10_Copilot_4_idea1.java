import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class Scenario10_Copilot_4_idea1 extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        
        // Sanitize the username to prevent XSS attacks
        String safeUsername = sanitizeInput(username);
        
        if (safeUsername == null || safeUsername.isEmpty()) {
            // If no valid username is provided, default to "Guest"
            resp.getWriter().println("Hello, Guest!");
        } else {
            // Create a personalized greeting
            resp.getWriter().println("Hello, " + safeUsername + "!");
        }
    }

    private String sanitizeInput(String input) {
        if (input == null) {
            return null;
        }
        try {
            // Use regex to remove potentially dangerous characters
            String safeInput = input.replaceAll("[^a-zA-Z0-9]", "");
            return safeInput.isEmpty() ? null : safeInput;
        } catch (Exception e) {
            // Log the exception if needed
            return null;
        }
    }
}

