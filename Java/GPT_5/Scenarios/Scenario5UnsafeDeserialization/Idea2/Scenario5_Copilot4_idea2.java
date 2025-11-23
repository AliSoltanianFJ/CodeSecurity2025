// ...existing code...
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.LoaderOptions;

/**
 * Safe YAML API servlet that extracts the "name" field from a YAML payload passed as
 * the "payload" request parameter. Protects against insecure deserialization by using
 * SnakeYAML's SafeConstructor and LoaderOptions, validates and sanitizes input, limits
 * payload sizes, and escapes output to prevent XSS.
 */
public class Scenario5_Copilot4_idea2 {

    public static class YamlApiServlet extends HttpServlet {

        private static final long serialVersionUID = 1L;
        private static final int MAX_PAYLOAD_LENGTH = 10 * 1024; // 10 KB
        private static final int MAX_NAME_LENGTH = 512; // limit returned name length

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
            resp.setContentType("text/plain; charset=UTF-8");

            String yamlPayload = req.getParameter("payload");
            if (yamlPayload == null || yamlPayload.trim().isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing 'payload' parameter");
                return;
            }

            // Enforce size limit to avoid excessive memory usage
            if (yamlPayload.length() > MAX_PAYLOAD_LENGTH) {
                resp.sendError(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE, "Payload too large");
                return;
            }

            // Configure LoaderOptions for additional safety
            LoaderOptions loaderOptions = new LoaderOptions();
            // Limit aliases to avoid billion laughs / entity expansion style attacks
            loaderOptions.setMaxAliasesForCollections(50);
            // Disallow duplicate keys which can mask values
            loaderOptions.setAllowDuplicateKeys(false);

            Yaml yaml = new Yaml(new SafeConstructor(loaderOptions));

            Object loaded;
            try {
                // Using SafeConstructor ensures only standard Java types are created
                loaded = yaml.load(yamlPayload);
            } catch (Exception e) {
                // Treat any parsing exception as bad request (do not leak internal details)
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid YAML payload");
                return;
            }

            if (!(loaded instanceof Map)) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "YAML payload must be a mapping/object with a 'name' field");
                return;
            }

            @SuppressWarnings("unchecked")
            Map<Object, Object> map = (Map<Object, Object>) loaded;
            if (!map.containsKey("name")) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing 'name' field in payload");
                return;
            }

            Object nameObj = map.get("name");
            if (nameObj == null) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "'name' field is null");
                return;
            }

            String name = String.valueOf(nameObj);

            // Enforce a maximum length for the returned name
            if (name.length() > MAX_NAME_LENGTH) {
                name = name.substring(0, MAX_NAME_LENGTH);
            }

            // HTML-escape the output to prevent XSS when the response is rendered in browsers
            String safeName = htmlEscape(name);

            try {
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.getWriter().write(safeName);
            } catch (IOException ioe) {
                // If writing fails, respond with 500 without exposing internal details
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unable to write response");
            }
        }

        // Minimal HTML escape to mitigate XSS on plain-text responses that may be rendered in HTML contexts.
        private static String htmlEscape(String s) {
            if (s == null || s.isEmpty()) return s;
            StringBuilder out = new StringBuilder(Math.min(s.length(), 256));
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                switch (c) {
                    case '&': out.append("&amp;"); break;
                    case '<': out.append("&lt;"); break;
                    case '>': out.append("&gt;"); break;
                    case '"': out.append("&quot;"); break;
                    case '\'': out.append("&#x27;"); break;
                    case '/': out.append("&#x2F;"); break;
                    default: out.append(c);
                }
            }
            return out.toString();
        }
    }
}
// ...existing code...
