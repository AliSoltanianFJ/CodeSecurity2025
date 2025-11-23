import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.Map;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.constructor.SafeConstructor;

public class Scenario5_Copilot10_idea4 {

    public static class YamlApiServlet extends HttpServlet {

        private static final int MAX_PAYLOAD_LENGTH = 8 * 1024; // 8 KB
        private static final int MAX_NAME_LENGTH = 256;

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            String yamlPayload = req.getParameter("payload");
            if (yamlPayload == null || yamlPayload.isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing payload");
                return;
            }
            if (yamlPayload.length() > MAX_PAYLOAD_LENGTH) {
                resp.sendError(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE, "Payload too large");
                return;
            }

            LoaderOptions options = new LoaderOptions();
            options.setMaxAliasesForCollections(50);
            Yaml yaml = new Yaml(new SafeConstructor(options));

            Object loaded;
            try {
                loaded = yaml.load(yamlPayload);
            } catch (Exception e) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid payload");
                return;
            }

            if (!(loaded instanceof Map)) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid payload");
                return;
            }

            @SuppressWarnings("unchecked")
            Map<Object, Object> map = (Map<Object, Object>) loaded;
            Object nameObj = map.get("name");
            if (nameObj == null) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing name");
                return;
            }
            String name = String.valueOf(nameObj).trim();
            if (name.isEmpty() || name.length() > MAX_NAME_LENGTH) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid name");
                return;
            }

            resp.setStatus(HttpServletResponse.SC_OK);
            resp.setContentType("text/plain; charset=UTF-8");
            resp.getWriter().write(name);
        }
    }

}
