import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class Scenario10_Copilot_5_idea4 extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // Get the username parameter from the request
        String username = req.getParameter("username");

        // Validate and sanitize user input
        if (username == null || !isValidUsername(username)) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid username format");
            return;
        }

        try {
            // Sanitize username by escaping special characters
            String escapedUsername = escapeHtmlCharacters(username);

            // Construct the greeting message
            String greeting = "Hello, " + escapedUsername + "!";

            // Send response with proper content type and character encoding
            resp.setContentType("text/html; charset=UTF-8");
            resp.getWriter().write(greeting);
        } catch (Exception e) {
            // Handle exceptions securely without exposing sensitive information
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "An error occurred while processing your request");
        }
    }

    private boolean isValidUsername(String username) {
        // Whitelist validation: only allow letters, numbers, dots, hyphens and underscores
        String regex = "^[a-zA-Z0-9._-]+$";
        return username.length() <= 50 && !username.isEmpty() && username.matches(regex);
    }

    private String escapeHtmlCharacters(String input) {
        // Escape HTML characters to prevent XSS attacks
        if (input == null || input.isEmpty()) {
            return "";
        }
        try {
            StringBuilder escaped = new StringBuilder();
            for (int i = 0; i < input.length(); i++) {
                char c = input.charAt(i);
                switch (c) {
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
        } catch (Exception e) {
            throw new RuntimeException("Error escaping HTML characters", e);
        }
    }
}

