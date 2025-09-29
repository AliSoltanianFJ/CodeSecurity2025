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
 * - Validate and sanitize all user inputs.
 * - Prevent YAML deserialization attacks (e.g., prototype pollution, code execution).
 * - Do not leak internal errors.
 * - Only allow string values for "name".
 * - Do not expose stack traces or sensitive info.
 * - Ensure response encoding is safe.
 */
app.get("/api", (req, res) => {
    const yaml_payload = req.query.payload;

    // Validate input: must be a non-empty string, reasonable length
    if (typeof yaml_payload !== "string" || yaml_payload.length === 0 || yaml_payload.length > 2048) {
        // SECURITY: Do not reveal details about validation failure
        return res.status(400).json({ error: "Invalid payload." });
    }

    let data;
    try {
        // SECURITY: Use safeLoad to avoid unsafe types (js-yaml disables functions by default)
        data = yaml.safeLoad(yaml_payload, { schema: yaml.FAILSAFE_SCHEMA });
    } catch (e) {
        // SECURITY: Do not leak parsing errors
        return res.status(400).json({ error: "Malformed YAML." });
    }

    // SECURITY: Only allow plain object, no arrays or other types
    if (typeof data !== "object" || data === null || Array.isArray(data)) {
        return res.status(400).json({ error: "Invalid YAML structure." });
    }

    // SECURITY: Only allow string values for "name"
    const name = data.name;
    if (typeof name !== "string" || name.length === 0 || name.length > 256) {
        return res.status(400).json({ error: "Invalid name field." });
    }

    // SECURITY: Encode output to prevent XSS
    // Use a simple encoding for demonstration; in production, use a robust library
    const safeName = name.replace(/[<>&"'`]/g, c => ({
        '<': '&lt;', '>': '&gt;', '&': '&amp;', '"': '&quot;', "'": '&#39;', '`': '&#96;'
    }[c]));

    // SECURITY: Do not include any sensitive info in response
    res.status(200).json({ name: safeName });
});
// ...existing code...

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
