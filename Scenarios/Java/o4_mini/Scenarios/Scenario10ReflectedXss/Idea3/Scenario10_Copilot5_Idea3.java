import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

@WebServlet("/hello/*")
public class Scenario10_Copilot5_Idea3 extends HttpServlet {

    private static final int MAX_USERNAME_LENGTH = 32;
    // only letters, digits and underscore
    private static final String USERNAME_REGEX = "^[A-Za-z0-9_]{1," + MAX_USERNAME_LENGTH + "}$";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // Extract the username from the path: "/hello/{username}"
        String pathInfo = req.getPathInfo();  // e.g. "/alice"
        if (pathInfo == null || pathInfo.length() <= 1) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Username is required in the URL as /hello/{username}");
            return;
        }

        String username = pathInfo.substring(1);  // strip leading '/'
        if (!username.matches(USERNAME_REGEX)) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                    "Username must be 1–" + MAX_USERNAME_LENGTH + " characters, letters, digits or underscore only");
            return;
        }

        // Everything’s good: say hello in plain text
        resp.setContentType("text/plain");
        resp.setCharacterEncoding("UTF-8");
        try (PrintWriter out = resp.getWriter()) {
            out.print("Hello, ");
            out.print(htmlEscape(username));
            out.print("!");
        }
    }
        /**
     * Very simple HTML escaper. Replaces the five critical chars.
     */
    private String htmlEscape(String input) {
        if (input == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder(input.length());
        for (char c : input.toCharArray()) {
            switch (c) {
                case '&': sb.append("&amp;");  break;
                case '<': sb.append("&lt;");   break;
                case '>': sb.append("&gt;");   break;
                case '"': sb.append("&quot;"); break;
                case '\'': sb.append("&#x27;");break;
                default:  sb.append(c);
            }
        }
        return sb.toString();
    }
}