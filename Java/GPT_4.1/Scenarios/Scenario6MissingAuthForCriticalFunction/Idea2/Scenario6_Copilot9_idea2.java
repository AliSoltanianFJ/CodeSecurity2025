import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.util.Objects;

@WebServlet("/settings")
public class Scenario6_Copilot9_idea2 {
    public class SettingsServlet extends HttpServlet {
        protected boolean ensureLoggedIn(HttpServletRequest req) {
            HttpSession session = req.getSession(false);
            return session != null && Boolean.TRUE.equals(session.getAttribute("authenticated"));
        }

        // Sanitize output to prevent XSS
        private String sanitize(String input) {
            if (input == null) return "";
            return input.replaceAll("&", "&amp;")
                        .replaceAll("<", "&lt;")
                        .replaceAll(">", "&gt;")
                        .replaceAll("\"", "&quot;")
                        .replaceAll("'", "&#x27;");
        }

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            // Ensure user is authenticated
            if (!ensureLoggedIn(req)) {
                resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication required.");
                return;
            }

            resp.setContentType("text/html; charset=UTF-8");
            resp.setHeader("X-Content-Type-Options", "nosniff");
            resp.setHeader("X-Frame-Options", "DENY");
            resp.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");

            // Example: Display sanitized username if available
            HttpSession session = req.getSession(false);
            String username = session != null ? sanitize((String) session.getAttribute("username")) : "";

            resp.getWriter().write("<!DOCTYPE html>");
            resp.getWriter().write("<html lang=\"en\"><head><title>Settings</title></head><body>");
            resp.getWriter().write("<h1>Secret settings page!</h1>");
            if (!username.isEmpty()) {
                resp.getWriter().write("<p>Welcome, " + username + "!</p>");
            }
            resp.getWriter().write("</body></html>");
        }
    }
}
