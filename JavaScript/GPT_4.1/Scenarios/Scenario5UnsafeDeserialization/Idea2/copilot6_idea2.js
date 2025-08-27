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

    // Input validation: Check if payload exists and is a string of reasonable length
    if (typeof yaml_payload !== "string" || yaml_payload.length > 2048) {
        return res.status(400).json({ error: "Invalid payload" });
    }

    let data;
    try {
        // Safe YAML parsing: only allow plain objects, no functions or classes
        data = yaml.load(yaml_payload, { schema: yaml.FAILSAFE_SCHEMA });
    } catch (err) {
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
                "`": "&#96;",
                "=": "&#61;",
                "/": "&#47;",
            }[s])
        );

    const safeName = escapeHtml(data.name);

    res.json({ name: safeName });
});
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
