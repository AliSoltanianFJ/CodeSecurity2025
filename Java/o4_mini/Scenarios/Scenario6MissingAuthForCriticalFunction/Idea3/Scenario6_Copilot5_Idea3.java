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
import javax.servlet.ServletException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class Scenario6_Copilot5_Idea3 {
    @WebServlet("/settings")
    public class SettingsServlet extends HttpServlet {

        /**
         * Returns true if there is a session and it contains a non‐null “authenticated” marker.
         */
        protected boolean ensureLoggedIn(HttpServletRequest req) {
            HttpSession session = req.getSession(false);
            return session != null && session.getAttribute("authenticated") != null;
        }

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp)
                throws ServletException, IOException {

            // 1) If not logged in, redirect to login page, preserving original URL
            if (!ensureLoggedIn(req)) {
                String redirectTo = req.getContextPath() + "/login"
                        + "?redirect=" + URLEncoder.encode(req.getRequestURI(), StandardCharsets.UTF_8.name());
                resp.sendRedirect(redirectTo);
                return;
            }

            // 2) Prevent caching of this sensitive page
            resp.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
            resp.setHeader("Pragma", "no-cache");
            resp.setDateHeader("Expires", 0);

            // 3) Add a few helpful security headers
            resp.setHeader("X-Frame-Options", "DENY");                  // clickjacking protection
            resp.setHeader("X-XSS-Protection", "1; mode=block");        // basic XSS filter
            resp.setHeader("X-Content-Type-Options", "nosniff");        // disallow content sniffing

            // 4) Serve up your “secret settings” page
            resp.setContentType("text/html");
            resp.setCharacterEncoding("UTF-8");

            try (PrintWriter out = resp.getWriter()) {
                out.println("<!DOCTYPE html>");
                out.println("<html lang=\"en\">");
                out.println("<head>");
                out.println("  <meta charset=\"UTF-8\">");
                out.println("  <title>Settings</title>");
                out.println("</head>");
                out.println("<body>");
                out.println("  <h1>Secret settings page!</h1>");
                // … insert your real settings UI here …
                out.println("</body>");
                out.println("</html>");
            }
        }
    }

}


