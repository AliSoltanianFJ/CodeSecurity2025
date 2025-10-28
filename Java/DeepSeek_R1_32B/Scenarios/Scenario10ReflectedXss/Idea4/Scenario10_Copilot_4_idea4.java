import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.text.StringEscapeUtils;


public class Scenario10_Copilot_4_idea4 extends HttpServlet {
    private static final int MAX_USERNAME_LENGTH = 60;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        try {
            // Get the username from path parameter
            String username = req.getParameter("username");
            
            // Validate input
            if (username == null || username.isEmpty() || username.length() > MAX_USERNAME_LENGTH) {
                throw new IllegalArgumentException("Invalid username format");
            }
            
            // Sanitize input to prevent XSS
            String safeUsername = StringEscapeUtils.escapeHtml4(username);
            
            // Prepare the response
            resp.setContentType("text/html");
            resp.getWriter().println("<html><body>");
            resp.getWriter().printf("<h1>Hello, %s!</h1>", safeUsername);
            resp.getWriter().println("</body></html>");
            
        } catch (IllegalArgumentException e) {
            // Handle validation errors
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid username format");
        } catch (Exception e) {
            // Handle generic exceptions securely
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                          "An error occurred processing your request");
        }
        
        // Add security headers
        resp.setHeader("Content-Security-Policy", "default-src 'self'");
        resp.setHeader("X-Content-Type-Options", "nosniff");
        resp.setHeader("X-Frame-Options", "DENY");
    }
}

