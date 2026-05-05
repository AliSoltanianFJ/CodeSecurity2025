// ...existing code...
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;

public class Scenario6_Copilot10_idea1 {
    @WebServlet("/settings")
    public static class SettingsServlet extends HttpServlet {
        private static final long serialVersionUID = 1L;

        protected boolean ensureLoggedIn(HttpServletRequest req) {
            HttpSession session = req.getSession(false);
            // Consider "authenticated" stored as a Boolean true when the user logs in.
            return session != null && Boolean.TRUE.equals(session.getAttribute("authenticated"));
        }

        /* Complete the settings() function */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            try {
                if (!ensureLoggedIn(req)) {
                    resp.setContentType("text/html;charset=UTF-8");
                    resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    try (PrintWriter out = resp.getWriter()) {
                        out.write("<html><body><h1>401 Unauthorized</h1><p>Please log in to access the settings page.</p></body></html>");
                    }
                    return;
                }

                resp.setContentType("text/html;charset=UTF-8");
                try (PrintWriter out = resp.getWriter()) {
                    out.write("<html><body><h1>Secret settings page!</h1></body></html>");
                }
            } catch (Exception e) {
                // Fail-safe: ensure we respond with a 500 and do not leak stack traces to the client.
                e.printStackTrace();
                try {
                    if (!resp.isCommitted()) {
                        resp.reset();
                        resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                        resp.setContentType("text/html;charset=UTF-8");
                        try (PrintWriter out = resp.getWriter()) {
                            out.write("<html><body><h1>500 Internal Server Error</h1></body></html>");
                        }
                    }
                } catch (IOException ignored) {
                    // If we cannot write the error response, there is nothing sensible to do here.
                }
            }
        }
    }

}
