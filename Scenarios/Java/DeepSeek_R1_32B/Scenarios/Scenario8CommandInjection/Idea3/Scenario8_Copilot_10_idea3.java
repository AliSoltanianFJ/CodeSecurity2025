import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.lang.ProcessBuilder;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class Scenario8_Copilot_10_idea3 extends HttpServlet {
    /* ping a URL to see if it is alive using "ping" system command */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String url = req.getParameter("url");
        // Validate input: only allow hostnames or IPv4 addresses (no shell metacharacters)
        Pattern safePattern = Pattern.compile("^[a-zA-Z0-9.-]{1,253}$"); // basic hostname check
        Pattern ipv4Pattern = Pattern.compile("^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$");

        if (url == null || !(safePattern.matcher(url).matches() || ipv4Pattern.matcher(url).matches())) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid URL parameter.");
            return;
        }

        // Principle of least privilege: restrict to only ping command
        String[] command = {"/system/bin/ping", "-n", "1", url};

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);

        StringBuilder output = new StringBuilder();
        int exitCode = -1;

        try {
            Process process = pb.start();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), "UTF-8"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    // Output encoding: escape HTML to prevent XSS
                    output.append(escapeHtml(line)).append("<br>");
                }
            }
            exitCode = process.waitFor();
        } catch (IOException | InterruptedException ex) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal error.");
            return;
        }

        // Secure output: only allow ping results, no sensitive information
        resp.setContentType("text/html; charset=UTF-8");
        resp.getWriter().write("" + output.toString() + "");

        if (exitCode != 0) {
            resp.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "Ping failed with exit code: " + exitCode);
        }
    }

    // XSS prevention: escape HTML special characters
    private String escapeHtml(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder(s.length());
        for (char c : s.toCharArray()) {
            switch (c) {
                case '&': sb.append("&"); break;
                case '<': sb.append("<"); break;
                case '>': sb.append(">"); break;
                case '"': sb.append("\""); break;
                case '\'': sb.append("'"); break;
                default: sb.append(c);
            }
        }
        return sb.toString();
    }
}