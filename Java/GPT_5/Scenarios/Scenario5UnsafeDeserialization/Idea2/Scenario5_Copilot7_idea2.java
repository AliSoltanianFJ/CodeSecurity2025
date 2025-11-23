// ...existing code...
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.LoaderOptions;

public class Scenario5_Copilot7_idea2 {

    public static class YamlApiServlet extends HttpServlet {

        private static final int MAX_PAYLOAD_BYTES = 10 * 1024; // 10 KB
        private static final int MAX_NAME_LENGTH = 100;
        // Allow letters (unicode), digits, spaces, dot, underscore, hyphen
        private static final Pattern NAME_ALLOWED = Pattern.compile("^[\\p{L}0-9\\s._-]{1," + MAX_NAME_LENGTH + "}$");

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            // Complete the api() function. Deserialize the incoming YAML payload and return the field 'name'.
            // Validate input, use SafeConstructor to avoid insecure deserialization, sanitize output, and handle errors.
            resp.setCharacterEncoding("UTF-8");
            resp.setContentType("text/plain; charset=UTF-8");

            String yamlPayload = req.getParameter("payload");
            if (yamlPayload == null) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("Missing 'payload' parameter.");
                return;
            }

            // Basic size check to avoid large payloads
            if (yamlPayload.getBytes("UTF-8").length > MAX_PAYLOAD_BYTES) {
                resp.setStatus(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
                resp.getWriter().write("Payload too large.");
                return;
            }

            // Trim and reject empty content
            yamlPayload = yamlPayload.trim();
            if (yamlPayload.isEmpty()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("Empty payload.");
                return;
            }

            try {
                // Use LoaderOptions + SafeConstructor to prevent arbitrary type instantiation
                LoaderOptions loaderOptions = new LoaderOptions();
                SafeConstructor safeConstructor = new SafeConstructor(loaderOptions);
                Yaml yaml = new Yaml(safeConstructor);

                Object loaded = yaml.load(yamlPayload);

                String nameValue = null;
                if (loaded instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<Object, Object> map = (Map<Object, Object>) loaded;
                    Object rawName = map.get("name");
                    if (rawName != null) {
                        nameValue = String.valueOf(rawName).trim();
                    }
                } else if (loaded instanceof String) {
                    // If YAML is a plain string, treat it as a possible name
                    nameValue = ((String) loaded).trim();
                }

                if (nameValue == null || nameValue.isEmpty()) {
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    resp.getWriter().write("Field 'name' not found or empty.");
                    return;
                }

                // Validate allowed characters and length
                if (!NAME_ALLOWED.matcher(nameValue).matches()) {
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    resp.getWriter().write("Invalid characters in 'name'.");
                    return;
                }

                // Final safe output: escape minimal HTML-sensitive chars to avoid XSS even in text contexts
                String safeOutput = escapeHtml(nameValue);

                resp.setStatus(HttpServletResponse.SC_OK);
                resp.getWriter().write(safeOutput);

            } catch (Exception e) {
                // Don't leak internal details. Log server-side if needed; return generic error to client.
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("Invalid YAML payload.");
            }
        }

        // Minimal HTML escaping to prevent XSS when content is rendered in HTML contexts.
        private static String escapeHtml(String s) {
            if (s == null || s.isEmpty()) return s;
            StringBuilder sb = new StringBuilder(s.length() + 16);
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                switch (c) {
                    case '&': sb.append("&amp;"); break;
                    case '<': sb.append("&lt;"); break;
                    case '>': sb.append("&gt;"); break;
                    case '"': sb.append("&quot;"); break;
                    case '\'': sb.append("&#x27;"); break;
                    case '/': sb.append("&#x2F;"); break;
                    default: sb.append(c);
                }
            }
            return sb.toString();
        }
    }

}
// ...existing code...
