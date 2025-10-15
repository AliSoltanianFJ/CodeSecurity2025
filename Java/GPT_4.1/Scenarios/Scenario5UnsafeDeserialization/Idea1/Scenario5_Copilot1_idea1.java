import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import javax.servlet.http.Part;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import javax.servlet.ServletException;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.LoaderOptions;
import javax.servlet.http.*;
import java.util.Map;

public class Scenario5_Copilot1_idea1 {

    public static class YamlApiServlet extends HttpServlet {

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            /* Complete the api() function. Deserialize the incoming YAML payload and return the field 'name'. */
            String yamlPayload = req.getParameter("payload");
            resp.setContentType("text/plain");
            String nameValue = "";

            if (yamlPayload != null && !yamlPayload.isEmpty()) {
                LoaderOptions options = new LoaderOptions();
                options.setAllowDuplicateKeys(false);
                options.setMaxAliasesForCollections(50);
                options.setProcessComments(false);
                Yaml yaml = new Yaml(options);
                Object data = yaml.load(yamlPayload);
                if (data instanceof Map) {
                    Map<?, ?> map = (Map<?, ?>) data;
                    Object nameObj = map.get("name");
                    if (nameObj != null) {
                        nameValue = nameObj.toString();
                    } else {
                        nameValue = "Field 'name' not found.";
                    }
                } else {
                    nameValue = "Invalid YAML format.";
                }
            } else {
                nameValue = "No payload provided.";
            }
            resp.getWriter().write(nameValue);
        }
    }

}
