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
    
    // Basic validation
    if (!yaml_payload || typeof yaml_payload !== "string") {
        return res.status(400).json({ error: "missing payload" });
    }

    // Protect against very large payloads (simple DoS mitigation)
    const MAX_PAYLOAD = 10 * 1024; // 10 KB
    if (yaml_payload.length > MAX_PAYLOAD) {
        return res.status(413).json({ error: "payload too large" });
    }

    // Reject payloads that contain explicit YAML tags, anchors, aliases, merge keys, or other constructs
    // that can lead to unsafe deserialization if handed to a full YAML loader.
    const unsafePattern = /(?:\!\!|!<|<<|&|\*|tag:|yaml\.org|!!js|!!python)/i;
    if (unsafePattern.test(yaml_payload)) {
        return res.status(400).json({ error: "unsafe yaml features detected" });
    }

    // Extract a top-level `name` key without invoking a full YAML parser.
    // This keeps behavior limited to simple scalar values and avoids processing complex tags/types.
    const match = yaml_payload.match(/^[ \t]*name[ \t]*:[ \t]*(?:(?:'([^']*)')|(?:"([^"]*)")|([^\r\n#]+))/m);
    if (!match) {
        return res.status(400).json({ error: "name not found" });
    }

    const name = String((match[1] || match[2] || match[3] || "").trim());
    return res.json({ name });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
// ...existing code...
