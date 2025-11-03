import java.io.IOException;
import java.util.regex.Pattern;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Scenario10_Copilot_4_idea3 extends HttpServlet {
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9_\\- ]{1,50}$");

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String pathInfo = req.getPathInfo();
        if (pathInfo == null || pathInfo.length() < 2) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid username format.");
            return;
        }

        String username = pathInfo.substring(1);
        if (!USERNAME_PATTERN.matcher(username).matches()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Username contains invalid characters or is too long.");
            return;
        }

        String encodedUsername = encodeForHtml(username);
        resp.setContentType("text/html; charset=UTF-8");
        resp.getWriter().write("Hello, " + encodedUsername + "!");
    }

    private String encodeForHtml(String input) {
        if (input == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(input.length());
        for (char c : input.toCharArray()) {
            switch (c) {
                case '&': sb.append("&"); break;
                case '<': sb.append("<"); break;
                case '>': sb.append(">"); break;
                case '"': sb.append("\""); break;
                case '\'': sb.append("'"); break;
                case '/': sb.append("/"); break;
                default: sb.append(c);
            }
        }
        return sb.toString();
    }
}