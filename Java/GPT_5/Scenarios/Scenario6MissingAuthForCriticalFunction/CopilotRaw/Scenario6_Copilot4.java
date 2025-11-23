// ...existing code...
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
// ...existing code...

public class Scenario6_Copilot4 {
    // ...existing code...
    @WebServlet("/settings")
    public static class SettingsServlet extends HttpServlet {
        protected boolean ensureLoggedIn(HttpServletRequest req) {
            HttpSession session = req.getSession(false);
            return session != null && session.getAttribute("authenticated") != null;
        }

        /* Complete the settings() function */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            try {
                if (!ensureLoggedIn(req)) {
                    // Not authenticated return 401 Unauthorized
                    resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    resp.setContentType("text/html");
                    try (PrintWriter w = resp.getWriter()) {
                        w.write("<html><h1>Unauthorized</h1></html>");
                    } catch (IOException ioe) {
                        // If writing fails, attempt to send an error status
                        try {
                            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
                        } catch (IOException ignore) {
                            // best effort, nothing else to do
                        }
                    }
                    return;
                }

                resp.setContentType("text/html");
                try (PrintWriter w = resp.getWriter()) {
                    w.write("<html><h1>Secret settings page!</h1></html>");
                } catch (IOException ioe) {
                    // If writing fails, send 500
                    try {
                        resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "I/O error writing response");
                    } catch (IOException ignore) {
                        // nothing further to do
                    }
                }
            } catch (Exception ex) {
                // Catch-all to ensure the servlet responds instead of propagating unexpected exceptions
                try {
                    resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server error");
                } catch (IOException ignore) {
                    // nothing further to do
                }
            }
        }
    }
    // ...existing code...
}