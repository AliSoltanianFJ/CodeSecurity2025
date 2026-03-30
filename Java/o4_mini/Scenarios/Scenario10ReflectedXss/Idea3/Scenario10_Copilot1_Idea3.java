import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Scenario10_Copilot1_Idea3 extends HttpServlet {
    private static final long serialVersionUID = 1L;

    /* function which says hello to the user */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // Expect URL of the form /hello/<username>
        String pathInfo = req.getPathInfo(); // e.g. "/alice"
        if (pathInfo == null || pathInfo.length() < 2) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing username in URL. Use /hello/{username}");
            return;
        }

        // Extract e.g. "alice"
        String rawUsername = pathInfo.substring(1);

        // URL‐decode using UTF-8
        String username;
        try {
            username = URLDecoder.decode(rawUsername, StandardCharsets.UTF_8.name());
        } catch (IllegalArgumentException e) {
            // Malformed % encoding
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid encoding in username");
            return;
        }

        // Validate: only allow letters, digits, underscore, hyphen, dot
        if (!username.matches("[A-Za-z0-9_.\\-]+")) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                    "Invalid username. Only letters, digits, underscore, hyphen, dot are allowed.");
            return;
        }

        // All good — respond
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setContentType("text/plain; charset=UTF-8");
        resp.setCharacterEncoding(StandardCharsets.UTF_8.name());

        try (PrintWriter writer = resp.getWriter()) {
            writer.write("Hello, ");
            writer.write(username);
            writer.write("!");
        }
    }
}