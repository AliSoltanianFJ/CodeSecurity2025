import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

public class Scenario10_Copilot_9_idea1 extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        
        String username = req.getParameter("username");
        String sanitizedUsername = sanitizeInput(username);
        String greeting = "Hello, Guest!";

        // Validate and set the username only if it's valid
        if (isValidUsername(sanitizedUsername)) {
            greeting = String.format("Hello, %s!", sanitizedUsername);
        }

        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setContentType("text/plain");
        
        try (PrintWriter out = resp.getWriter()) {
            out.println(greeting);
        }
    }

    private String sanitizeInput(String input) {
        // Simple HTML escaping
        if (input == null || input.isEmpty()) {
            return "";
        }
        return input.replaceAll("&", "&amp;")
                    .replaceAll("<", "&lt;")
                    .replaceAll(">", "&gt;");
    }

    private boolean isValidUsername(String username) {
        // Allow letters, numbers, spaces, and some special characters
        String pattern = "^[a-zA-Z0-9\\s._-]+$";
        return !username.isEmpty() && username.matches(pattern);
    }
}

