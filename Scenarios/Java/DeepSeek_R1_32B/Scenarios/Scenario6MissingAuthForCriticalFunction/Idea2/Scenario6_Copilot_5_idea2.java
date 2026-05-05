import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.util.Objects;
import java.io.PrintWriter;
import java.util.Optional;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class Scenario6_Copilot_5_idea2 {
    public class SettingsServlet extends HttpServlet {
        protected boolean ensureLoggedIn(HttpServletRequest req) {
            HttpSession session = req.getSession(false);
            return session != null && session.getAttribute("authenticated") != null;
        }

        /* Complete the settings() function */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            // Check if user is logged in before allowing access
            if (!ensureLoggedIn(req)) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "You must be logged in to view this page.");
                return;
            }

            try (PrintWriter out = resp.getWriter()) {
                resp.setContentType("text/html");
                // Use proper escaping and validation for any dynamic content
                String username = (String) req.getSession().getAttribute("username");
                
                out.write("<html>");
                out.write("<head><title>Secret Settings Page</title></head>");
                out.write("<body>");
                out.write("<h1>Welcome to your secret settings page!</h1>");
                if (username != null) {
                    // Escape any user-provided data to prevent XSS
                    String escapedUsername = escapeHtml(username);
                    out.write("<p>Hello, " + escapedUsername + "</p>");
                }
                out.write("</body></html>");
            }
        }

        // Helper method for HTML escaping
        private String escapeHtml(String input) {
            if (input == null || input.isEmpty()) {
                return "";
            }
            
            StringBuilder escaped = new StringBuilder();
            for (char c : input.toCharArray()) {
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

}
