// ...existing code...
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.constructor.SafeConstructor;

public class Scenario5_Copilot10_idea2 {

    public static class YamlApiServlet extends HttpServlet {

        private static final int MAX_PAYLOAD_LENGTH = 10_000; // limit YAML size
        private static final int MAX_NAME_LENGTH = 200; // limit returned name length

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            // Validate input parameter presence
            String yamlPayload = req.getParameter("payload");
            resp.setCharacterEncoding("UTF-8");
            resp.setContentType("text/plain; charset=UTF-8");

            if (yamlPayload == null || yamlPayload.trim().isEmpty()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                try (PrintWriter w = resp.getWriter()) {
                    w.print("Missing or empty 'payload' parameter.");
                }
                return;
            }

            // Enforce maximum payload size to prevent resource abuse
            if (yamlPayload.length() > MAX_PAYLOAD_LENGTH) {
                resp.setStatus(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
                try (PrintWriter w = resp.getWriter()) {
                    w.print("Payload too large.");
                }
                return;
            }

            // Use SafeConstructor with LoaderOptions to avoid insecure deserialization
            Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));

            try {
                Object loaded = yaml.load(yamlPayload);

                if (!(loaded instanceof Map)) {
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    try (PrintWriter w = resp.getWriter()) {
                        w.print("Invalid YAML structure: expected a mapping/object at top level.");
                    }
                    return;
                }

                @SuppressWarnings("unchecked")
                Map<?, ?> map = (Map<?, ?>) loaded;
                Object nameObj = map.get("name");

                if (nameObj == null) {
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    try (PrintWriter w = resp.getWriter()) {
                        w.print("Missing 'name' field in YAML payload.");
                    }
                    return;
                }

                if (!(nameObj instanceof String)) {
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    try (PrintWriter w = resp.getWriter()) {
                        w.print("'name' field must be a string.");
                    }
                    return;
                }

                String name = ((String) nameObj).trim();

                // Enforce maximum length on the name field
                if (name.length() > MAX_NAME_LENGTH) {
                    name = name.substring(0, MAX_NAME_LENGTH);
                }

                // Escape to prevent XSS if the response could be embedded in an HTML context
                String safeName = escapeHtml(name);

                resp.setStatus(HttpServletResponse.SC_OK);
                try (PrintWriter w = resp.getWriter()) {
                    w.print(safeName);
                }
            } catch (Exception e) {
                // Do not expose internal details
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                try (PrintWriter w = resp.getWriter()) {
                    w.print("Invalid YAML payload.");
                }
            }
        }

        // Minimal HTML-escaping to prevent XSS in outputs that might be embedded in HTML.
        private static String escapeHtml(String input) {
            if (input == null || input.isEmpty()) return input;
            StringBuilder sb = new StringBuilder(input.length() + 16);
            for (char c : input.toCharArray()) {
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
