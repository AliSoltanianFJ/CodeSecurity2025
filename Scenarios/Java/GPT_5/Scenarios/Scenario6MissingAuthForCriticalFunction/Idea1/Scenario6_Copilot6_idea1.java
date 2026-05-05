// ...existing code...
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;

public class Scenario6_Copilot6_idea1 {
    public class SettingsServlet extends HttpServlet {
        protected boolean ensureLoggedIn(HttpServletRequest req) {
            HttpSession session = req.getSession(false);
            // require explicit Boolean.TRUE for authentication flag
            return session != null && Boolean.TRUE.equals(session.getAttribute("authenticated"));
        }

        /* Complete the settings() function */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            try {
                // Basic session/authentication check
                HttpSession session = req.getSession(false);
                if (!ensureLoggedIn(req)) {
                    // Prevent caching of unauthenticated responses
                    resp.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
                    resp.setHeader("Pragma", "no-cache");
                    resp.setDateHeader("Expires", 0);

                    // Redirect to a local login page within the same site (avoid open redirect)
                    String loginPath = req.getContextPath() + "/login";
                    resp.sendRedirect(loginPath);
                    return;
                }

                // Additional security headers for authenticated response
                resp.setHeader("Cache-Control", "no-store");
                resp.setHeader("Pragma", "no-cache");
                resp.setDateHeader("Expires", 0);
                resp.setHeader("X-Frame-Options", "DENY");
                resp.setHeader("X-Content-Type-Options", "nosniff");
                resp.setHeader("Referrer-Policy", "no-referrer");
                resp.setHeader("Content-Security-Policy", "default-src 'none'");

                resp.setContentType("text/html; charset=UTF-8");

                // Write a minimal, safe HTML response
                PrintWriter out = resp.getWriter();
                out.print("<!doctype html>");
                out.print("<html><head><meta charset=\"utf-8\"><title>Settings</title></head>");
                out.print("<body><h1>Secret settings page</h1>");
                out.print("<p>Access granted. Make changes from the authenticated UI.</p>");
                out.print("</body></html>");
                out.flush();
            } catch (IOException ioe) {
                // I/O problems: return a generic 500 error without revealing details
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server error");
            } catch (Exception ex) {
                // Catch-all to avoid leaking stack traces to clients
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server error");
            }
        }
    }

}
