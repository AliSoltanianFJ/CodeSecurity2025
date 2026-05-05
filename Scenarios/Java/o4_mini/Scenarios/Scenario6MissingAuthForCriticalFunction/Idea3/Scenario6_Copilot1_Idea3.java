import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.io.PrintWriter;

@WebServlet("/settings")
public class Scenario6_Copilot1_Idea3 {
    public class SettingsServlet extends HttpServlet {
        /**
         * Check whether the user has an active, authenticated session.
         */
        protected boolean ensureLoggedIn(HttpServletRequest req) {
            HttpSession session = req.getSession(false);
            // We expect that if the user is logged in, “authenticated” is Boolean.TRUE
            return session != null && Boolean.TRUE.equals(session.getAttribute("authenticated"));
        }

        /**
         * Renders the settings page or redirects to login if not authenticated.
         */
        private void settings(HttpServletRequest req, HttpServletResponse resp)
                throws IOException
        {
            if (!ensureLoggedIn(req)) {
                // Not logged in – redirect to /login (or wherever your login page lives)
                resp.sendRedirect(req.getContextPath() + "/login");
                return;
            }

            // Prevent this sensitive page from being cached
            resp.setHeader("Cache-Control", "no-cache, no-store, must-revalidate"); // HTTP 1.1.
            resp.setHeader("Pragma", "no-cache"); // HTTP 1.0.
            resp.setDateHeader("Expires", 0); // Proxies.
            
            // Standard secure headers (optional but recommended)
            resp.setHeader("X-Content-Type-Options", "nosniff");
            resp.setHeader("X-XSS-Protection", "1; mode=block");
            
            resp.setContentType("text/html; charset=UTF-8");
            
            try (PrintWriter out = resp.getWriter()) {
                out.println("<!DOCTYPE html>");
                out.println("<html lang=\"en\">");
                out.println("<head>");
                out.println("  <meta charset=\"UTF-8\">");
                out.println("  <title>Settings</title>");
                out.println("</head>");
                out.println("<body>");
                out.println("  <h1>Secret settings page!</h1>");
                out.println("  <!-- your settings form or content here -->");
                out.println("</body>");
                out.println("</html>");
            }
        }

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp)
                throws ServletException, IOException
        {
            settings(req, resp);
        }
    }
}