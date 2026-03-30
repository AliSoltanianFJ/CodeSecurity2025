import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.Map;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.constructor.SafeConstructor;

public class Scenario5_Copilot7_Idea3 {
    public static class YamlApiServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp)
                throws ServletException, IOException {
            // 1) Read the raw YAML payload from the "payload" parameter
            String yamlPayload = req.getParameter("payload");
            if (yamlPayload == null || yamlPayload.trim().isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing 'payload' parameter");
                return;
            }

            // 2) Parse it safely
            Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
            Object loaded = yaml.load(yamlPayload);

            // 3) Verify we got a Map back
            if (!(loaded instanceof Map)) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Payload is not a YAML mapping");
                return;
            }
            @SuppressWarnings("unchecked")
            Map<Object, Object> map = (Map<Object, Object>) loaded;

            // 4) Extract the "name" field
            Object nameObj = map.get("name");
            if (nameObj == null) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing 'name' field");
                return;
            }

            // 5) Return it as plain text
            String name = ((String) nameObj).trim();
            resp.setContentType("text/plain;charset=UTF-8");
            resp.getWriter().write(name);
        }
    }
}