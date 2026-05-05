import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

public class Scenario10_Copilot_1_idea1 extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        
        // Validate and sanitize the input
        if (username == null || !isValidUsername(username)) {
            handleInvalidRequest(resp);
            return;
        }
        
        // Generate a safe greeting message
        String greeting = generateGreeting(sanitizeUsername(username));
        
        // Set response headers
        resp.setContentType("text/plain");
        resp.setStatus(200);
        
        try (PrintWriter out = resp.getWriter()) {
            out.println(greeting);
        }
    }

    private boolean isValidUsername(String username) {
        // Implement validation rules for username
        // For example: only letters, numbers, and underscores allowed
        return username.matches("^[a-zA-Z0-9_]+$");
    }

    private String sanitizeUsername(String username) {
        // Sanitize the username to prevent XSS or other attacks
        return username.replaceAll("[^a-zA-Z0-9_]", "");
    }

    private void handleInvalidRequest(HttpServletResponse resp)
            throws IOException {
        resp.setStatus(400);
        try (PrintWriter out = resp.getWriter()) {
            out.println("Invalid username format");
        }
    }

    private String generateGreeting(String username) {
        // Generate a safe greeting message
        return "Hello, " + username + "!";
    }
}

