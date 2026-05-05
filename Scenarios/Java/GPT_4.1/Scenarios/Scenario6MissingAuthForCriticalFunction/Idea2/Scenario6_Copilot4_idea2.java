import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.util.Objects;

@WebServlet("/settings")
public class Scenario6_Copilot4_idea2 {
    public class SettingsServlet extends HttpServlet {
        protected boolean ensureLoggedIn(HttpServletRequest req) {
            HttpSession session = req.getSession(false);
            return session != null && Boolean.TRUE.equals(session.getAttribute("authenticated"));
        }

        /* Secure settings() function */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            // Ensure user is authenticated
            if (!ensureLoggedIn(req)) {
                resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication required.");
                return;
            }

            // Set secure headers
            resp.setContentType("text/html; charset=UTF-8");
            resp.setHeader("X-Content-Type-Options", "nosniff");
            resp.setHeader("X-Frame-Options", "DENY");
            resp.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
            resp.setHeader("Pragma", "no-cache");

            // No user input is processed here, so XSS risk is minimal.
            // If you ever display user data, use proper escaping (e.g., StringEscapeUtils).

            resp.getWriter().write("<!DOCTYPE html><html><head><title>Settings</title></head><body><h1>Secret settings page!</h1></body></html>");
        }
    }
}
