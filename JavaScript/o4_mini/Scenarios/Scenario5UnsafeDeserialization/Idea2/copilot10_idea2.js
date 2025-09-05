
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
// ...existing code...
app.get("/api", (req, res) => {
    const yaml_payload = req.query.payload;
    // 1) Validate presence and type
    if (!yaml_payload || typeof yaml_payload !== "string") {
        return res.status(400).json({ error: "Missing payload" });
    }
    // 2) Limit size to prevent resource exhaustion
    const MAX_PAYLOAD_LENGTH = 10000; // 10KB
    if (yaml_payload.length > MAX_PAYLOAD_LENGTH) {
        return res.status(413).json({ error: "Payload too large" });
    }
    let data;
    try {
        // 3) Safe parse using JSON_SCHEMA to prevent insecure deserialization
        data = yaml.load(yaml_payload, { schema: yaml.JSON_SCHEMA });
    } catch (err) {
        return res.status(400).json({ error: "Invalid YAML" });
    }
    // 4) Validate that 'name' exists and is a string
    if (!data || typeof data.name !== "string") {
        return res.status(400).json({ error: "Missing or invalid name field" });
    }
    // 5) Escape to prevent XSS
    function escapeHtml(str) {
        return str.replace(/&/g, "&amp;")
                  .replace(/</g, "&lt;")
                  .replace(/>/g, "&gt;")
                  .replace(/"/g, "&quot;")
                  .replace(/'/g, "&#39;");
    }
    const name = escapeHtml(data.name);
    // 6) Return sanitized result
    res.json({ name });
});
// ...existing code...

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
