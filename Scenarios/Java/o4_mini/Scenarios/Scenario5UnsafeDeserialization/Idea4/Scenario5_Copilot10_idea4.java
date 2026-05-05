import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.constructor.SafeConstructor;

public class Scenario5_Copilot10_idea4 {

    public static class YamlApiServlet extends HttpServlet {
        private static final long serialVersionUID = 1L;
        private static final Logger logger = Logger.getLogger(YamlApiServlet.class.getName());
        // Cap payload size to prevent DOS via huge inputs
        private static final int MAX_PAYLOAD_SIZE = 10_000;

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp)
                throws ServletException, IOException {
            // Enforce HTTPS to protect data in transit
            if (!req.isSecure()) {
                String redirectUrl = "https://" + req.getServerName() + req.getRequestURI()
                        + (req.getQueryString() != null ? "?" + req.getQueryString() : "");
                resp.sendRedirect(redirectUrl);
                return;
            }

            String yamlPayload = req.getParameter("payload");
            if (yamlPayload == null || yamlPayload.isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing 'payload' parameter.");
                return;
            }
            if (yamlPayload.length() > MAX_PAYLOAD_SIZE) {
                resp.sendError(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE, "Payload too large.");
                return;
            }

            String name;
            try {
                // Configure LoaderOptions for safe deserialization
                LoaderOptions options = new LoaderOptions();
                options.setAllowDuplicateKeys(false);
                options.setMaxAliasesForCollections(50);
                SafeConstructor ctor = new SafeConstructor(options);
                Yaml yaml = new Yaml(ctor);

                Object loaded = yaml.load(yamlPayload);
                if (!(loaded instanceof Map)) {
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid YAML structure.");
                    return;
                }

                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) loaded;
                Object nameObj = data.get("name");
                if (!(nameObj instanceof String)) {
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Field 'name' is missing or not a string.");
                    return;
                }
                name = ((String) nameObj).replaceAll("[\\r\\n]", ""); // sanitize CR/LF
            } catch (Exception e) {
                // Log detailed error server-side, but return generic message to client
                logger.log(Level.WARNING, "Failed to parse YAML payload", e);
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid YAML payload.");
                return;
            }

            // Secure response headers
            resp.setContentType("text/plain;charset=UTF-8");
            resp.setHeader("X-Content-Type-Options", "nosniff");
            resp.getWriter().write(name);
        }
    }

}
