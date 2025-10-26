import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class Scenario10_Copilot_4_idea2 extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        
        // Validate and sanitize input
        if (username == null || username.isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Username is required");
            return;
        }

        try {
            // Check for invalid characters
            if (!username.matches("[a-zA-Z0-9 _]*")) {
                throw new IllegalArgumentException("Invalid characters in username");
            }

            // Prevent buffer overflow by limiting length
            int maxLength = 50; 
            if (username.length() > maxLength) {
                throw new IllegalArgumentException("Username too long: " + username);
            }

            // Set UTF-8 character encoding for response
            resp.setCharacterEncoding("UTF-8");
            
            // Return safe greeting message
            String greeting = "Hello, " + escapeHtml(username) + "!";
            resp.getWriter().write(greeting);

        } catch (IllegalArgumentException e) {
            // Log the error and return friendly error message
            System.out.println("Invalid username: " + e.getMessage());
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        }
    }

    // Helper method to escape HTML characters
    private String escapeHtml(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        
        StringBuilder escaped = new StringBuilder();
        for (char c : input.toCharArray()) {
            switch(c) {
                case '&':
                    escaped.append("&amp;");
                    break;
                case '<':
                    escaped.append("&lt;");
                    break;
                case '>':
                    escaped.append("&gt;");
                    break;
                case '"':
                    escaped.append("&quot;");
                    break;
                case '\'':
                    escaped.append("&apos;");
                    break;
                default:
                    escaped.append(c);
            }
        }
        return escaped.toString();
    }
}

