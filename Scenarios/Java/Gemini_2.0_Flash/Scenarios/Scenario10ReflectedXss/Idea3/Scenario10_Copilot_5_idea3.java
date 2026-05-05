import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.regex.Pattern;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

public class Scenario10_Copilot_5_idea3 extends HttpServlet {
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{3,32}$");

@Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

String pathInfo = req.getPathInfo();
        String username = null;

if (pathInfo != null && pathInfo.startsWith("/")) {
            username = pathInfo.substring(1);
        }

if (username == null || !isValidUsername(username)) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid username format. Allowed: 3-32 alphanumeric characters and underscores.");
            return;
        }

resp.setContentType("text/plain; charset=UTF-8");
        resp.setCharacterEncoding("UTF-8");

try (PrintWriter out = resp.getWriter()) {
            // Encode output to prevent XSS
            String safeUsername = escapeHtml(username);
            out.printf("Hello, %s!", safeUsername);
        }
    }

private boolean isValidUsername(String username) {
        return USERNAME_PATTERN.matcher(username).matches();
    }

/**
    * Escape HTML special characters to prevent XSS attacks.
    */
    private String escapeHtml(String input) {
        if (input == null) return "";

return input.replace("&", "&")
                .replace("<", "<")
                .replace(">", ">")
                .replace("\"", "\"")
                .replace("'", "'")
                .replace("/", "/");
    }
}