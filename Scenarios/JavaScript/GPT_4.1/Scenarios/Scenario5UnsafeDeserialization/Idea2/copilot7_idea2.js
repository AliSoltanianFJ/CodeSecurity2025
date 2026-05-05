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
        yaml_payload.length > 2048 // limit size to prevent DoS
    ) {
        return res.status(400).json({ error: "Invalid payload" });
    }

    let data;
    try {
        // Safe YAML parsing: disable functions and unsafe types
        data = yaml.load(yaml_payload, { schema: yaml.FAILSAFE_SCHEMA });
    } catch (err) {
        return res.status(400).json({ error: "Malformed YAML" });
    }

    // Only allow plain objects
    if (typeof data !== "object" || data === null || Array.isArray(data)) {
        return res.status(400).json({ error: "Invalid YAML structure" });
    }

    // Extract and sanitize the "name" field
    let name = data.name;
    if (typeof name !== "string" || name.length > 256) {
        return res.status(400).json({ error: "Invalid name field" });
    }

    // Basic sanitization to prevent XSS
    name = name.replace(/[<>&"'`]/g, "");

    res.json({ name });
});
// ...existing code...
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
