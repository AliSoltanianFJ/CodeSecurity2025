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

    // Input validation: Check if payload exists and is a string of reasonable length
    if (typeof yaml_payload !== 'string' || yaml_payload.length > 2048) {
        return res.status(400).json({ error: "Invalid payload" });
    }

    let data;
    try {
        // Safe YAML parsing: disable functions and unsafe types
        data = yaml.load(yaml_payload, { schema: yaml.FAILSAFE_SCHEMA });
    } catch (err) {
        return res.status(400).json({ error: "Malformed YAML" });
    }

    // Validate and sanitize the "name" field
    let name = "";
    if (data && typeof data.name === "string") {
        // Basic sanitization: remove potentially dangerous characters
        name = data.name.replace(/[<>&"'`]/g, "");
        // Limit length to prevent buffer overflow/XSS
        name = name.substring(0, 256);
    } else {
        return res.status(400).json({ error: "Missing or invalid 'name' field" });
    }

    // Respond with sanitized name
    res.json({ name });
});
// ...existing code...
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
