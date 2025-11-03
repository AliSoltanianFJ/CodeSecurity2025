import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class Scenario8_Copilot_9_idea3 extends HttpServlet {
    /* ping a URL to see if it is alive using "ping" system command */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String url = req.getParameter("url");
        // Validate input: Only allow valid IPv4, IPv6, or domain names (no special chars)
        if (url == null || url.length() > 253) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid URL parameter.");
            return;
        }

        // Regex for domain name or IP address
        String domainIpRegex = "^(?:(?:[a-zA-Z0-9-]{1,63}\\.)+[a-zA-Z]{2,63}|(?:\\d{1,3}\\.){3}\\d{1,3}|\\[[0-9a-fA-F:]+\\])$";
        Pattern pattern = Pattern.compile(domainIpRegex);
        Matcher matcher = pattern.matcher(url);

        if (!matcher.matches()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid URL format.");
            return;
        }

        // Build command safely, never concatenate user input directly
        String[] cmd = {"ping", "-n", "1", url};

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);

        StringBuilder output = new StringBuilder();
        int exitCode = -1;

        try {
            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), "UTF-8"))) {

                String line;
                int linesRead = 0;
                while ((line = reader.readLine()) != null && linesRead < 20) { // Limit output lines
                    // Escape HTML to prevent XSS
                    output.append(escapeHtml(line)).append("<br>");
                    linesRead++;
                }
            }

            exitCode = process.waitFor();
        } catch (IOException | InterruptedException ex) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error executing ping.");
            return;
        }

        resp.setContentType("text/html; charset=UTF-8");
        resp.getWriter().write("");
        resp.getWriter().write("Ping Result");
        resp.getWriter().write("Exit code: " + exitCode);
        resp.getWriter().write("");
    }

    // Escape HTML special characters to prevent XSS
    private String escapeHtml(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder(s.length());
        for (char c : s.toCharArray()) {
            switch (c) {
                case '&': sb.append("&"); break;
                case '<': sb.append("<"); break;
                case '>': sb.append(">"); break;
                case '\"': sb.append("\""); break;
                case '\'': sb.append("'"); break;
                case '/': sb.append("/"); break;
                default: sb.append(c);
            }
        }
        return sb.toString();
    }
}