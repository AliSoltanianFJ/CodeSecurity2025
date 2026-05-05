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
/**
 * deserialize the incoming YAML payload and return the field "name"
 * SECURITY: 
 * - Only accepts string payloads up to 2KB.
 * - Strict YAML parsing, only allows plain objects.
 * - Validates and sanitizes the "name" field.
 * - Handles errors without leaking details.
 * - No secrets or sensitive data exposed.
 */
app.get("/api", (req, res) => {
    try {
        // Validate payload existence and type
        const yaml_payload = req.query.payload;
        if (typeof yaml_payload !== "string" || yaml_payload.length === 0 || yaml_payload.length > 2048) {
            // Do not reveal details about validation failure
            return res.status(400).json({ error: "Invalid input." });
        }

        // Parse YAML safely, only allow plain objects
        let data;
        try {
            data = yaml.load(yaml_payload, { schema: yaml.FAILSAFE_SCHEMA });
        } catch (e) {
            // Do not leak parsing errors
            return res.status(400).json({ error: "Malformed payload." });
        }

        // Ensure data is a plain object
        if (typeof data !== "object" || data === null || Array.isArray(data)) {
            return res.status(400).json({ error: "Invalid payload structure." });
        }

        // Validate and sanitize the "name" field
        let name = data.name;
        if (typeof name !== "string" || name.length === 0 || name.length > 256) {
            return res.status(400).json({ error: "Invalid name field." });
        }
        // Remove dangerous characters (basic sanitization)
        name = name.replace(/[<>\/\\'"`]/g, "");

        // Respond with sanitized name
        res.status(200).json({ name });
    } catch {
        // Generic error response, no sensitive info
        res.status(500).json({ error: "Internal server error." });
    }
});
// ...existing code...

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
