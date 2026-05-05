import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import javax.servlet.ServletException;

public class Copilot7_Idea3 extends HttpServlet {

    private static final long serialVersionUID = 1L;

    /**
     * Expects requests of the form GET /hello/{username}.
     * Replies with “Hello, {username}!” in text/plain UTF-8.
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        // Extract the path info: "/{username}"
        String pathInfo = req.getPathInfo();

        if (pathInfo == null || pathInfo.length() <= 1) {
            // nothing after the slash
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Username is missing");
            return;
        }

        // remove leading slash
        String username = pathInfo.substring(1);

        // validate: allow only letters, digits, underscore and hyphen
        if (!username.matches("[A-Za-z0-9_-]+")) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid username");
            return;
        }

        // set content type and encoding
        resp.setContentType("text/plain");
        resp.setCharacterEncoding("UTF-8");

        // write the greeting
        resp.getWriter().write("Hello, " + username + "!");
    }
}
