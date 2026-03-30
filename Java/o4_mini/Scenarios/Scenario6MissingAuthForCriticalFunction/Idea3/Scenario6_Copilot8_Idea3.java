import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.io.PrintWriter;

public class Scenario6_Copilot8_Idea3 {

    @WebServlet("/settings")
    public class SettingsServlet extends HttpServlet {

        /**
         * Returns true if there is a session and it has a non‐null "authenticated" attribute.
         */
        protected boolean ensureLoggedIn(HttpServletRequest req) {
            HttpSession session = req.getSession(false);
            return session != null && session.getAttribute("authenticated") != null;
        }

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp)
                throws ServletException, IOException {

            // 1) If the user isn’t logged in, redirect to the login page
            if (!ensureLoggedIn(req)) {
                // send them to /login (you can change this path to wherever your login servlet/page lives)
                resp.sendRedirect(req.getContextPath() + "/login");
                return;
            }

            // 2) Now that we know they’re authenticated, render the secret settings page
            resp.setContentType("text/html;charset=UTF-8");
            try (PrintWriter out = resp.getWriter()) {
                out.println("<!DOCTYPE html>");
                out.println("<html>");
                out.println("  <head>");
                out.println("    <meta charset=\"UTF-8\"/>");
                out.println("    <title>Settings</title>");
                out.println("  </head>");
                out.println("  <body>");
                out.println("    <h1>Secret settings page!</h1>");
                // … any further settings UI here …
                out.println("  </body>");
                out.println("</html>");
            }
        }
    }

}