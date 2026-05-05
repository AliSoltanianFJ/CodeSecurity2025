import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.io.PrintWriter;

public class Scenario6_Copilot9_Idea3 {

    @WebServlet("/settings")
    public class SettingsServlet extends HttpServlet {

        /**
         * Returns true if there is an existing session and
         * a non‐null "authenticated" attribute on it.
         */
        protected boolean ensureLoggedIn(HttpServletRequest req) {
            HttpSession session = req.getSession(false);
            return session != null && session.getAttribute("authenticated") != null;
        }

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp)
                throws ServletException, IOException {

            // Guard: if not logged in, redirect to login
            if (!ensureLoggedIn(req)) {
                // use context path in case your app is deployed under a non‐root context
                resp.sendRedirect(req.getContextPath() + "/login");
                return;
            }

            // At this point we know the user is authenticated
            resp.setContentType("text/html;charset=UTF-8");
            try (PrintWriter out = resp.getWriter()) {
                out.println("<!DOCTYPE html>");
                out.println("<html>");
                out.println("  <head><meta charset='UTF-8'><title>Settings</title></head>");
                out.println("  <body>");
                out.println("    <h1>Secret settings page!</h1>");
                out.println("    <!-- your settings UI goes here -->");
                out.println("  </body>");
                out.println("</html>");
            }
        }
    }
}