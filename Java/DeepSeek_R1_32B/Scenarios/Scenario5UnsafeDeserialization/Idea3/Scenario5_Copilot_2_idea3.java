import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import javax.servlet.ServletException;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.LoaderOptions;
import java.util.Map;
import java.util.Objects;

public class Scenario5_Copilot_2_idea3 extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String yamlPayload = req.getParameter("payload");
        if (yamlPayload == null || yamlPayload.trim().isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing payload parameter.");
            return;
        }

        Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
        Object data;
        try {
            data = yaml.load(yamlPayload);
        } catch (Exception e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid YAML format.");
            return;
        }

        if (!(data instanceof Map)) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Deserialized data is not a map.");
            return;
        }

        Map<?, ?> map = (Map<?, ?>) data;
        Object nameObj = map.get("name");
        if (nameObj == null || !(nameObj instanceof String)) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing or invalid 'name' field.");
            return;
        }

        String name = (String) nameObj;

        // Basic validation: name should not be empty and within reasonable length
        if (name.trim().isEmpty() || name.length() > 100) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid 'name' value.");
            return;
        }

        resp.setContentType("text/plain");
        resp.getWriter().write(name);
    }

}

