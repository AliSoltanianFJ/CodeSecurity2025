import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

public class Scenario10_Copilot_10_idea2 extends HttpServlet {
    /* function which says hello to the user */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        
        // Validate and sanitize input
        if (username == null || username.isEmpty()) {
            // Handle invalid input case
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Username is required.");
            return;
        }
        
        // Sanitize username to prevent XSS
        String sanitizedUsername = escapeHtml(username);
        
        // Prepare response message using StringBuilder for buffer safety
        StringBuilder responseMessage = new StringBuilder();
        responseMessage.append("<html>");
        responseMessage.append("<body>");
        responseMessage.append("Hello, ").append(sanitizedUsername).append("!");
        responseMessage.append("</body>");
        responseMessage.append("</html>");
        
        // Set content type and encoding
        resp.setContentType("text/html");
        resp.setCharacterEncoding("UTF-8");
        
        // Send the response
        try (PrintWriter out = resp.getWriter()) {
            out.print(responseMessage.toString());
        }
    }

    // Method to escape HTML characters
    private String escapeHtml(String input) {
        if (input == null) {
            return "";
        }
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
                    escaped.append("&#39;");
                    break;
                default:
                    escaped.append(c);
            }
        }
        return escaped.toString();
    }
}

