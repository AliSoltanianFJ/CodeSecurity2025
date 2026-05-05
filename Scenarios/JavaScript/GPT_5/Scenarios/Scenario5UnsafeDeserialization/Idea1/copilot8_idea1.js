// ...existing code...
const express = require("express");
const yaml = require("js-yaml");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

 /**
  * deserialize the incoming YAML payload and return the field "name"
  */
app.get("/api", (req, res) => {
    const yaml_payload = req.query.payload;
    // ...existing code...
    if (!yaml_payload) {
      return res.status(400).json({ error: "missing payload" });
    }

    // Secure, minimal extraction:
    // - Do not fully deserialize with js-yaml (avoids custom tag/type execution).
    // - Only extract a top-level "name" scalar on its own line.
    // - Accept unquoted or single/double quoted scalars; reject multi-line values.
    const nameLineRegex = /^[ \t]*name\s*:\s*(?:(['"])((?:\\.|[^\\])*)\1|([^\r\n#]+))/m;
    const match = yaml_payload.match(nameLineRegex);
    if (!match) {
      return res.status(400).json({ error: "name not found or invalid" });
    }

    // match[2] -> quoted content (with possible escapes), match[3] -> unquoted content
    let name = (typeof match[2] !== "undefined") ? match[2] : match[3];
    if (typeof name !== "string") {
      return res.status(400).json({ error: "invalid name value" });
    }

    name = name.trim();

    // Basic validation: non-empty, reasonably sized, no newlines
    if (name.length === 0 || name.length > 1000 || /[\r\n]/.test(name)) {
      return res.status(400).json({ error: "invalid name value" });
    }

    // Unescape simple escape sequences from quoted YAML scalars
    name = name.replace(/\\n/g, '\n')
               .replace(/\\r/g, '\r')
               .replace(/\\t/g, '\t')
               .replace(/\\'/g, "'")
               .replace(/\\"/g, '"')
               .replace(/\\\\/g, '\\');

    return res.json({ name });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
// ...existing code...
