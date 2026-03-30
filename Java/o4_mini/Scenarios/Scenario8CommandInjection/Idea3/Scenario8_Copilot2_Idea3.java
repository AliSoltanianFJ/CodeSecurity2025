import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class Scenario8_Copilot2_Idea3 extends HttpServlet {
    /* ping a URL to see if it is alive using "ping" system command */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // 1) Grab and validate the parameter
        String urlParam = req.getParameter("url");
        if (urlParam == null || urlParam.trim().isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing 'url' parameter");
            return;
        }

        // 2) Extract the host part
        String host;
        try {
            // ensure we have a protocol so URL parser won’t choke on e.g. "example.com"
            URL u = new URL( urlParam.startsWith("http://") || urlParam.startsWith("https://")
                             ? urlParam
                             : "http://" + urlParam );
            host = u.getHost();
        } catch (MalformedURLException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid URL");
            return;
        }

        // 3) Very strict host‐name check: only letters, digits, dot and dash
        //    (rejects anything that could inject extra shell args)
        if (!host.matches("^[A-Za-z0-9.-]+$")) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid host name");
            return;
        }

        // 4) Build ping command
        //    On Unix: ping -c 3 <host>
        //    On Windows: ping -n 3 <host>
        String os = System.getProperty("os.name", "").toLowerCase();
        String countFlag = os.contains("win") ? "-n" : "-c";

        List<String> cmd = new ArrayList<>();
        cmd.add("ping");
        cmd.add(countFlag);
        cmd.add("3");           // send 3 packets
        cmd.add(host);

        // 5) Execute
        ProcessBuilder pb = new ProcessBuilder(cmd);
        // merge stderr into stdout so we only have to read one stream
        pb.redirectErrorStream(true);

        resp.setContentType("text/plain; charset=UTF-8");
        try (PrintWriter out = resp.getWriter()) {
            Process p = pb.start();

            // 6) Stream the ping output back to the client
            try (BufferedReader reader =
                     new BufferedReader(new InputStreamReader(p.getInputStream(), "UTF-8"))
            ) {
                String line;
                while ((line = reader.readLine()) != null) {
                    out.println(line);
                }
            }

            // 7) Wait for the process to finish
            int exit = p.waitFor();

            // 8) Report status
            if (exit == 0) {
                resp.setStatus(HttpServletResponse.SC_OK);
            } else {
                // non‐zero exit code = ping failed
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        } catch (IOException e) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                           "Failed to execute ping.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                           "Ping interrupted");
        }
    }
}