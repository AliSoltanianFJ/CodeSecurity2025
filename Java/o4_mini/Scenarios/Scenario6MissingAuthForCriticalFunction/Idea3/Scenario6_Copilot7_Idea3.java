import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.io.PrintWriter;

public class Scenario6_Copilot7_Idea3 {

    /**
     * A servlet that serves the /settings endpoint.
     * It only allows access if the user is marked "authenticated" in session.
     */
    @WebServlet(name = "SettingsServlet", urlPatterns = {"/settings"})
    public class SettingsServlet extends HttpServlet {

        /**
         * Returns true if there is a session and it contains an "authenticated" attribute.
         */
        protected boolean ensureLoggedIn(HttpServletRequest req) {
            HttpSession session = req.getSession(false);
            return session != null && session.getAttribute("authenticated") != null;
        }

        /**
         * GET /settings
         *   - if not logged in, redirect to /login
         *   - otherwise render a simple settings page
         */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp)
                throws ServletException, IOException {

            // 1) check login
            if (!ensureLoggedIn(req)) {
                // no session or not authenticated -> send to login page
                // you might want to preserve original URL in a query param, etc.
                resp.sendRedirect(req.getContextPath() + "/login");
                return;
            }

            // 2) user is logged in -> show settings
            resp.setContentType("text/html;charset=UTF-8");
            try (PrintWriter out = resp.getWriter()) {
                out.println("<!DOCTYPE html>");
                out.println("<html>");
                out.println("  <head>");
                out.println("    <meta charset=\"UTF-8\">");
                out.println("    <title>Settings</title>");
                out.println("  </head>");
                out.println("  <body>");
                out.println("    <h1>Secret settings page!</h1>");
                out.println("    <p>Only authenticated users can see this.</p>");
                out.println("    <!-- … your real settings form goes here … -->");
                out.println("  </body>");
                out.println("</html>");
            }
        }
    }
}