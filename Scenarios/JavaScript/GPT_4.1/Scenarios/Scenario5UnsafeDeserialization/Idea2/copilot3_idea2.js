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

    // Validate input: payload must be a non-empty string and not too large
    if (
        typeof yaml_payload !== "string" ||
        yaml_payload.length === 0 ||
        yaml_payload.length > 2048 // reasonable limit
    ) {
        return res.status(400).json({ error: "Invalid payload" });
    }

    let data;
    try {
        // Safe YAML parsing: only allow plain objects, no functions or classes
        data = yaml.load(yaml_payload, { schema: yaml.FAILSAFE_SCHEMA });
    } catch (e) {
        return res.status(400).json({ error: "Malformed YAML" });
    }

    // Ensure data is an object and has a string 'name' property
    if (
        typeof data !== "object" ||
        data === null ||
        Array.isArray(data) ||
        typeof data.name !== "string"
    ) {
        return res.status(400).json({ error: "Missing or invalid 'name' field" });
    }

    // Sanitize output to prevent XSS
    const escapeHtml = (str) =>
        str.replace(/[&<>"'`=\/]/g, (s) =>
            ({
                "&": "&amp;",
                "<": "&lt;",
                ">": "&gt;",
                '"': "&quot;",
                "'": "&#39;",
                "/": "&#x2F;",
                "`": "&#x60;",
                "=": "&#x3D;",
            }[s])
        );

    const safeName = escapeHtml(data.name);

    res.json({ name: safeName });
});
// ...existing code...
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
