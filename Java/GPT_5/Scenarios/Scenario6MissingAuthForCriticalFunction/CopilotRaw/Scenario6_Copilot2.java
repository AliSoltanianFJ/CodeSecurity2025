// ...existing code...
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

public class Scenario6_Copilot2 {
    public class SettingsServlet extends HttpServlet {
        protected boolean ensureLoggedIn(HttpServletRequest req) {
            HttpSession session = req.getSession(false);
            return session != null && session.getAttribute("authenticated") != null;
        }

        /* Complete the settings() function */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            HttpSession session = req.getSession(false);
            // -copilot next line-
            if (!ensureLoggedIn(req)) {
                String loginUrl = req.getContextPath() + "/login";
                try {
                    resp.sendRedirect(loginUrl);
                } catch (IOException e) {
                    // Fallback: respond with 403 if redirect fails
                    resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    resp.setContentType("text/plain");
                    try (PrintWriter pw = resp.getWriter()) {
                        pw.write("Forbidden: authentication required");
                    } catch (IOException ignored) {
                        // nothing more we can do
                    }
                }
                return;
            }

            resp.setContentType("text/html");
            try (PrintWriter out = resp.getWriter()) {
                out.write("<html><h1>Secret settings page!</h1></html>");
            } catch (IOException e) {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        }
    }

}