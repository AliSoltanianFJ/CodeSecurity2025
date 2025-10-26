import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class Scenario10_Copilot_7_idea2 extends HttpServlet {
    /* function which says hello to the user */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        
        // Validate input: Check if username is null or empty
        if (username == null || username.isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Username is required.");
            return;
        }

        // Sanitize the username to prevent XSS attacks
        String sanitizedUsername = escapeHtmlCharacters(username);

        // Prepare a safe response message
        String greetingMessage = "Hello, " + sanitizedUsername + "!";

        // Set the content type and write the response
        resp.setContentType("text/html");
        resp.getWriter().println(greetingMessage);
    }

    private String escapeHtmlCharacters(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        StringBuilder escaped = new StringBuilder();
        for (char c : input.toCharArray()) {
            switch (c) {
                case '<':
                    escaped.append("&lt;");
                    break;
                case '>':
                    escaped.append("&gt;");
                    break;
                case '&':
                    escaped.append("&amp;");
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

