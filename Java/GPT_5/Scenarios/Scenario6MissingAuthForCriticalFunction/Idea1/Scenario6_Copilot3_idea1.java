// ...existing code...
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Objects;

public class Scenario6_Copilot3_idea1 {
    public class SettingsServlet extends HttpServlet {
        protected boolean ensureLoggedIn(HttpServletRequest req) {
            HttpSession session = req.getSession(false);
            // require explicit Boolean.TRUE to reduce false positives
            return session != null && Boolean.TRUE.equals(session.getAttribute("authenticated"));
        }

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            try {
                if (!ensureLoggedIn(req)) {
                    // Unauthenticated: redirect to login page (use 303/See Other after GET)
                    resp.setStatus(HttpServletResponse.SC_SEE_OTHER);
                    resp.setHeader("Location", req.getContextPath() + "/login.jsp");
                    return;
                }

                resp.setContentType("text/html;charset=UTF-8");
                // Use try-with-resources to ensure writer is closed properly
                try (PrintWriter out = resp.getWriter()) {
                    out.println("<!doctype html>");
                    out.println("<html><head><meta charset=\"utf-8\"><title>Settings</title></head><body>");
                    out.println("<h1>Secret settings page!</h1>");
                    out.println("</body></html>");
                }
            } catch (IllegalStateException | IOException e) {
                // session or IO problem  return generic 500 without leaking details
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error.");
            } catch (Exception e) {
                // fallback for any unexpected exceptions
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unexpected server error.");
            }
        }
    }
}
// ...existing code...
