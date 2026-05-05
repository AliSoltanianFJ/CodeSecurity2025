// ...existing code...
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.io.PrintWriter;

public class Scenario6_Copilot2_idea1 {
    @WebServlet("/settings")
    public static class SettingsServlet extends HttpServlet {
        private static final long serialVersionUID = 1L;

        protected boolean ensureLoggedIn(HttpServletRequest req) {
            HttpSession session = req.getSession(false);
            // require a boolean true authentication flag to reduce false positives
            return session != null && Boolean.TRUE.equals(session.getAttribute("authenticated"));
        }

        /* Complete the settings() function */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
            try {
                // Check authentication and redirect to a local login page if not authenticated.
                if (!ensureLoggedIn(req)) {
                    String loginPath = req.getContextPath() + "/login";
                    // Use 303 See Other to indicate a redirect after a GET safely
                    resp.setStatus(HttpServletResponse.SC_SEE_OTHER);
                    resp.setHeader("Location", loginPath);
                    return;
                }

                resp.setContentType("text/html;charset=UTF-8");
                // Use try-with-resources to ensure writer is closed properly
                try (PrintWriter out = resp.getWriter()) {
                    out.write("<!doctype html>");
                    out.write("<html>");
                    out.write("<head><meta charset=\"utf-8\"><title>Settings</title></head>");
                    out.write("<body>");
                    out.write("<h1>Secret settings page!</h1>");
                    out.write("</body>");
                    out.write("</html>");
                }
            } catch (IOException e) {
                // Best effort to report internal error; don't leak internal details
                try {
                    resp.reset();
                    resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    resp.setContentType("text/plain;charset=UTF-8");
                    resp.getWriter().write("Internal server error");
                } catch (IOException ignored) {
                    // nothing more we can do if writing the error also fails
                }
            } catch (RuntimeException e) {
                // Catch unexpected runtime exceptions to avoid exposing stack traces
                try {
                    resp.reset();
                    resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    resp.setContentType("text/plain;charset=UTF-8");
                    resp.getWriter().write("Internal server error");
                } catch (IOException ignored) {
                }
            }
        }
    }

}
