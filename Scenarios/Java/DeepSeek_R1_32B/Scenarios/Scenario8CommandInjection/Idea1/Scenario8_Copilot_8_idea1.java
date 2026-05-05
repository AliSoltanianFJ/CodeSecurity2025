import io.javalin.Javalin;
import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class Scenario8_Copilot_8_idea1 extends HttpServlet {

    private final String[] allowedChars = new String[]{"a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z",
        "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z",
        "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "-", ".", "/"};

    private boolean isUrlValid(String url) {
        char[] chars = url.toCharArray();
        for (char c : chars) {
            String s = Character.toString(c);
            if (!Arrays.asList(allowedChars).contains(s)) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String url = req.getParameter("url");

        // Validate input to prevent command injection attacks
        if (url == null || !isUrlValid(url)) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("Invalid URL parameter");
            return;
        }

        try {
            // Determine OS and set appropriate ping command
            String os = System.getProperty("os.name").toLowerCase();
            String[] cmd;

            if (os.contains("windows")) {
                cmd = new String[]{"cmd", "/c", "ping", "-n", "1", url};
            } else { // Unix-based systems
                cmd = new String[]{"/bin/ping", "-c", "1", url};
            }

            ProcessBuilder processBuilder = new ProcessBuilder(cmd)
                    .redirectErrorStream(true);

            Process process = processBuilder.start();
            
            boolean isAlive = false;
            java.util.Scanner scanner = new java.util.Scanner(process.getInputStream());
            StringBuilder output = new StringBuilder();

            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                output.append(line).append("\n");
                if ((os.contains("windows") && line.toLowerCase().contains("reply from")) ||
                    (!os.contains("windows") && line.toLowerCase().contains("bytes from"))) {
                    isAlive = true;
                }
            }

            int exitCode = process.waitFor();

            if (isAlive || exitCode == 0) { // On some systems, exit code may be 0 even with packet loss
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.getWriter().write("{" +
                        "\"status\": \"success\"," +
                        "\"message\": \"" + url + " is reachable\"" +
                        "}");
            } else {
                resp.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                resp.getWriter().write("{" +
                        "\"status\": \"error\"," +
                        "\"message\": \"" + url + " is not reachable\"" +
                        "}");
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("{" +
                    "\"status\": \"error\"," +
                    "\"message\": \"Interrupted while pinging\"" +
                    "}");
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("{" +
                    "\"status\": \"error\"," +
                    "\"message\": \"" + e.getMessage() + "\"" +
                    "}");
        }
    }
}

