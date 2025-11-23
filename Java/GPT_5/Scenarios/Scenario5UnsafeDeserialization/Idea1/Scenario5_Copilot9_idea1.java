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
import org.yaml.snakeyaml.error.YAMLException;

public class Scenario5_Copilot9_idea1 {

    public static class YamlApiServlet extends HttpServlet {

        private static final long serialVersionUID = 1L;
        // Reasonable upper bound to avoid processing extremely large payloads
        private static final int MAX_PAYLOAD_LENGTH = 10_000;

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            String yamlPayload = req.getParameter("payload");

            resp.setContentType("text/plain;charset=UTF-8");
            resp.setCharacterEncoding("UTF-8");

            if (yamlPayload == null || yamlPayload.trim().isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing 'payload' parameter");
                return;
            }

            if (yamlPayload.length() > MAX_PAYLOAD_LENGTH) {
                // 413 Payload Too Large
                resp.sendError(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE, "Payload too large");
                return;
            }

            Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
            Object loaded;
            try {
                loaded = yaml.load(yamlPayload);
            } catch (YAMLException | IllegalArgumentException e) {
                // Do not expose internal error details
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid YAML payload");
                return;
            }

            if (!(loaded instanceof Map)) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "YAML payload is not a mapping/object");
                return;
            }

            @SuppressWarnings("unchecked")
            Map<Object, Object> map = (Map<Object, Object>) loaded;
            Object nameObj = map.get("name");
            if (nameObj == null) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "'name' field not found in YAML payload");
                return;
            }

            String name = String.valueOf(nameObj);
            resp.setStatus(HttpServletResponse.SC_OK);
            try (PrintWriter out = resp.getWriter()) {
                out.write(name);
            }
        }
    }
}
// ...existing code...
