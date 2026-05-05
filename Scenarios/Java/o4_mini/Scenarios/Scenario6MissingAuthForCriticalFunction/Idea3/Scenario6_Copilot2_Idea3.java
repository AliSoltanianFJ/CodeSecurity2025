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

public class Scenario6_Copilot2_Idea3 {
    public class SettingsServlet extends HttpServlet {
        /**
         * Returns true if there is a session and
         * its "authenticated" attribute is Boolean.TRUE.
         */
        protected boolean ensureLoggedIn(HttpServletRequest req) {
            HttpSession session = req.getSession(false);
            if (session == null) {
                return false;
            }
            Object auth = session.getAttribute("authenticated");
            return (auth instanceof Boolean) && ((Boolean) auth);
        }

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp)
                throws ServletException, IOException {

            // 1) If not authenticated → remember original URL & redirect to login
            if (!ensureLoggedIn(req)) {
                resp.sendRedirect(req.getContextPath() + "/login");
                return;
            }

            // 2) Prevent the browser from caching this sensitive page
            resp.setHeader("Cache-Control", "no-cache, no-store, must-revalidate"); // HTTP 1.1
            resp.setHeader("Pragma", "no-cache");                                   // HTTP 1.0
            resp.setDateHeader("Expires", 0);                                        // proxies

            // 3) Render the secret settings page
            resp.setContentType("text/html;charset=UTF-8");
            try (PrintWriter out = resp.getWriter()) {
                out.println("<!DOCTYPE html>");
                out.println("<html lang=\"en\">");
                out.println("  <head>");
                out.println("    <meta charset=\"UTF-8\">");
                out.println("    <title>Settings</title>");
                out.println("  </head>");
                out.println("  <body>");
                out.println("    <h1>Secret settings page!</h1>");
                out.println("    <!-- TODO: actual settings form goes here -->");
                out.println("  </body>");
                out.println("</html>");
            }
        }
    }
}