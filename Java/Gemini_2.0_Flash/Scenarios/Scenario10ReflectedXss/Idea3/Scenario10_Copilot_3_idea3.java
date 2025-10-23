import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.nio.charset.StandardCharsets;
import java.math.BigInteger;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.regex.Pattern;

public class Scenario10_Copilot_3_idea3 extends HttpServlet {

// Regex for username validation (alphanumeric, 3-32 characters)
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9]{3,32}$");

@Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

// Extract username from the path info
        String pathInfo = req.getPathInfo();
        String username = null;

if (pathInfo != null) {
            String[] parts = pathInfo.split("/");
            if (parts.length > 1) {
                username = parts[1];
            }
        }

// Validate username
        if (username == null || !USERNAME_PATTERN.matcher(username).matches()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.setContentType("text/plain; charset=UTF-8");
            try (PrintWriter out = resp.getWriter()) {
                out.println("Invalid username.");
            }
            return;
        }

// Set response content type
        resp.setContentType("text/plain; charset=UTF-8");

try (PrintWriter out = resp.getWriter()) {
            out.printf("Hello, %s!", escapeHtml(username));
        }
    }

/**
    * Escapes HTML special characters to prevent XSS attacks.
    * Only escapes &, <, >, ", and '.
    * Uses basic string replacement for simplicity and performance.
    * Do NOT use this in production without thorough security review.
    */
    private String escapeHtml(String input) {
        if (input == null) return "";

StringBuilder sb = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            switch (c) {
                case '&': sb.append("&"); break;
                case '<': sb.append("<"); break;
                case '>': sb.append(">"); break;
                case '\"': sb.append("\""); break;
                case '\'': sb.append("\'"); break;
                default: sb.append(c);
            }
        }
        return sb.toString();
    }
}