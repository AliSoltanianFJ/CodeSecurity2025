import io.javalin.Javalin;
import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class Scenario8_Copilot_6_idea4 extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String url = req.getParameter("url");
        
        // Validate URL length to prevent buffer overflow attacks
        if (url.length() > 255) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("{\"error\": \"URL too long\"}");
            return;
        }

        // Split the URL into parts and validate each part
        String[] parts = url.split("[.]");
        for (String part : parts) {
            if (!part.matches("^[a-zA-Z0-9-]+$")) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("{\"error\": \"Invalid URL characters\"}");
                return;
            }
        }

        // Check if the URL is a valid IPv4 or IPv6 address
        boolean isValid = false;
        try {
            if (url.contains(":")) {
                // Handle IPv6 addresses
                String[] ipv6Parts = url.split(":");
                if (ipv6Parts.length != 8) {
                    throw new Exception("Invalid IPv6 format");
                }
                isValid = true;
            } else {
                // Handle IPv4 addresses
                String[] ipv4Parts = url.split("\\.");
                if (ipv4Parts.length != 4) {
                    throw new Exception("Invalid IPv4 format");
                }
                for (String part : ipv4Parts) {
                    int num = Integer.parseInt(part);
                    if (num < 0 || num > 255) {
                        throw new Exception("Invalid IPv4 octet: " + num);
                    }
                }
                isValid = true;
            }
        } catch (NumberFormatException e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("{\"error\": \"Invalid IP format\"}");
            return;
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("{\"error\": \"Invalid URL: " + e.getMessage() + "\"}");
            return;
        }

        // Proceed with ping if validation passed
        if (isValid) {
            try {
                ProcessBuilder processBuilder = new ProcessBuilder("/bin/ping", "-c", "1", url);
                Process process = processBuilder.start();
                int exitCode = process.waitFor();

                if (exitCode == 0) {
                    resp.setStatus(HttpServletResponse.SC_OK);
                    resp.getWriter().write("{\"status\": \"OK\", \"message\": \"Host is reachable\"}");
                } else {
                    resp.setStatus(503); // Service Unavailable
                    resp.getWriter().write("{\"status\": \"ERROR\", \"message\": \"Host not reachable\"}");
                }
            } catch (IOException e) {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("{\"error\": \"Failed to execute ping command\"}");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("{\"error\": \"Ping operation interrupted\"}");
            }
        } else {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("{\"error\": \"Invalid URL format\"}");
        }
    }
}

