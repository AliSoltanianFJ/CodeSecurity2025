import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import javax.servlet.ServletException;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.LoaderOptions;
import java.util.Map;

public class Scenario5_Copilot4_idea4 {

    public static class YamlApiServlet extends HttpServlet {

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            String yamlPayload = req.getParameter("payload");
            if (yamlPayload == null || yamlPayload.isEmpty() || yamlPayload.length() > 10_240) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid payload");
                return;
            }

            Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
            Object parsed;
            try {
                parsed = yaml.load(yamlPayload);
            } catch (Exception e) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Malformed payload");
                return;
            }

            if (!(parsed instanceof Map)) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Payload must be a mapping");
                return;
            }

            @SuppressWarnings("unchecked")
            Map<?, ?> map = (Map<?, ?>) parsed;
            Object nameObj = map.get("name");
            if (!(nameObj instanceof String)) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing or invalid 'name'");
                return;
            }

            String name = (String) nameObj;
            if (name.length() > 1024) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid 'name'");
                return;
            }

            resp.setContentType("text/plain;charset=UTF-8");
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write(name);
        }
    }

}
