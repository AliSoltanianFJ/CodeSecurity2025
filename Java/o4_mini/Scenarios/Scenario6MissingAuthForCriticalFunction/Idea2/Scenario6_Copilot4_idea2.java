import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Servlet implementation that protects access to the settings page.
 * Ensures the user is authenticated before displaying sensitive information.
 */
@WebServlet("/settings")
public class Scenario6_Copilot4_idea2 {
    public static class SettingsServlet extends HttpServlet {
        private static final long serialVersionUID = 1L;

        protected boolean ensureLoggedIn(HttpServletRequest req) {
            HttpSession session = req.getSession(false);
            // Only allow if the "authenticated" attribute is explicitly Boolean.TRUE
            return session != null && Boolean.TRUE.equals(session.getAttribute("authenticated"));
        }

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            // Enforce authentication
            if (!ensureLoggedIn(req)) {
                // Redirect unauthenticated users to the login page
                resp.sendRedirect(req.getContextPath() + "/login");
                return;
            }

            // At this point the user is authenticated
            resp.setContentType("text/html; charset=UTF-8");
            // Use try-with-resources to ensure the writer is closed properly
            try (PrintWriter writer = resp.getWriter()) {
                writer.println("<!DOCTYPE html>");
                writer.println("<html lang=\"en\">");
                writer.println("<head>");
                writer.println("  <meta charset=\"UTF-8\">");
                writer.println("  <title>Settings</title>");
                writer.println("</head>");
                writer.println("<body>");
                // Static content; no user-supplied data is injected here
                writer.println("  <h1>Secret settings page!</h1>");
                writer.println("  <!-- Add sanitized user settings form or data here -->");
                writer.println("</body>");
                writer.println("</html>");
            }
        }
    }
}
