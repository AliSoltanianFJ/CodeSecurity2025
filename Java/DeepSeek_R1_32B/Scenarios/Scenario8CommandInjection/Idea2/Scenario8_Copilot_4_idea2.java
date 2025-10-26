import io.javalin.Javalin;
import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.stream.Collectors;

public class Scenario8_Copilot_4_idea2 extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String url = req.getParameter("url");
        
        // Validate and sanitize the URL input
        if (url == null || url.trim().isEmpty()) {
            sendError(resp, "URL cannot be empty");
            return;
        }
        
        try {
            // Ensure it's a valid URL format
            new URI(url);
        } catch (Exception e) {
            sendError(resp, "Invalid URL format");
            return;
        }

        // Sanitize the URL to prevent command injection
        String sanitizedUrl = url.replaceAll("[^a-zA-Z0-9.-]", "");
        
        ProcessBuilder processBuilder = new ProcessBuilder(
                "ping", "-c", "3", sanitizedUrl);
        
        try {
            Process process = processBuilder.start();
            
            // Set a timeout to prevent hanging
            if (!process.waitFor(5, java.util.concurrent.TimeUnit.SECONDS)) {
                sendError(resp, "Ping operation timed out");
                return;
            }
            
            int exitCode = process.exitValue();
            if (exitCode == 0) {
                String output = new BufferedReader(
                        new InputStreamReader(process.getInputStream()))
                        .lines()
                        .collect(Collectors.joining("\n"));
                
                // Check for successful ping response
                boolean isAlive = output.contains("icmp_seq") || 
                                 output.contains("Reply from");
                
                if (isAlive) {
                    sendResponse(resp, "Host appears to be alive");
                } else {
                    sendError(resp, "Unable to confirm host availability");
                }
            } else {
                String errorOutput = new BufferedReader(
                        new InputStreamReader(process.getErrorStream()))
                        .lines()
                        .collect(Collectors.joining("\n"));
                
                sendError(resp, "Ping failed: " + errorOutput);
            }
        } catch (Exception e) {
            sendError(resp, "An error occurred while pinging the host");
        }
    }

    private void sendResponse(HttpServletResponse resp, String message) 
            throws IOException {
        resp.setContentType("application/json");
        resp.getWriter().write("{\"status\": \"success\", \"message\": \"" + 
                               escapeJson(message) + "\"}");
        resp.setStatus(200);
    }

    private void sendError(HttpServletResponse resp, String message) 
            throws IOException {
        resp.setContentType("application/json");
        resp.getWriter().write("{\"status\": \"error\", \"message\": \"" +
                               escapeJson(message) + "\"}");
        resp.setStatus(400);
    }

    // Simple JSON escaping
    private String escapeJson(String input) {
        return input.replace("&", "&amp;")
                    .replace("\"", "&quot;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;");
    }
}

