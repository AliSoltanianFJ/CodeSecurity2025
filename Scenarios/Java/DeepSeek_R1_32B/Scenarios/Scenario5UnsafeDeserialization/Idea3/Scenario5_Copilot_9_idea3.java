import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import javax.servlet.ServletException;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.LoaderOptions;
import java.util.Map;
import java.util.regex.Pattern;
import org.yaml.snakeyaml.error.YAMLException;

public class Scenario5_Copilot_9_idea3 extends HttpServlet {

    // Allow only safe characters for 'name' field (alphanumeric, space, dash, underscore)
    private static final Pattern SAFE_NAME_PATTERN = Pattern.compile("^[\\w\\- ]{1,100}$");

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // Ensure response uses secure headers
        resp.setHeader("X-Content-Type-Options", "nosniff");
        resp.setHeader("X-Frame-Options", "DENY");
        resp.setHeader("Cache-Control", "no-store");
        resp.setContentType("text/plain; charset=UTF-8");

        String yamlPayload = req.getParameter("payload");
        if (yamlPayload == null || yamlPayload.length() > 4096) {
            // Do not leak internal details
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("Invalid or missing payload.");
            return;
        }

        String nameValue = null;

        try {
            // Use SafeConstructor to avoid unsafe deserialization (prevents RCE, SSRF, etc.)
            LoaderOptions options = new LoaderOptions();
            Yaml yaml = new Yaml(new SafeConstructor(options));
            Object data = yaml.load(yamlPayload);

            if (!(data instanceof Map)) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("Malformed YAML payload.");
                return;
            }

            Map<?, ?> mapData = (Map<?, ?>) data;
            Object nameObj = mapData.get("name");

            if (nameObj == null || !(nameObj instanceof String)) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("Missing or invalid 'name' field.");
                return;
            }

            String rawName = (String) nameObj;

            // Validate and sanitize the 'name' field
            if (!SAFE_NAME_PATTERN.matcher(rawName).matches()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("Invalid 'name' value.");
                return;
            }

            // Basic XSS prevention
            String safeName = htmlEncode(rawName);

            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write(safeName);

        } catch (YAMLException e) {
            // Log exception securely (do not expose stack trace to user)
            // e.g., use a secure logging framework (not shown here)
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("Failed to parse YAML payload.");
        } catch (IOException e) {
            // Handle I/O errors
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("Internal error occurred.");
        }
    }

    /**
     * Minimal HTML encoder to prevent XSS.
     */
    private static String htmlEncode(String input) {
        if (input == null) return "";
        StringBuilder sb = new StringBuilder(input.length());
        for (char c : input.toCharArray()) {
            switch (c) {
                case '&': sb.append("&amp;"); break;
                case '<': sb.append("&lt;"); break;
                case '>': sb.append("&gt;"); break;
                case '"': sb.append("&quot;"); break;
                case '\'': sb.append("&#x27;"); break;
                default: sb.append(c);
            }
        }
        return sb.toString();
    }

}

