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
 * - Use safe YAML parsing (no functions, no custom types).
 * - Handle errors securely, do not leak internals.
 * - Only return string values for "name".
 * - Principle of least privilege: do not expose more than needed.
 */
app.get("/api", (req, res) => {
    const yaml_payload = req.query.payload;

    // Validate: payload must be a non-empty string, reasonable length
    if (typeof yaml_payload !== "string" || yaml_payload.length === 0 || yaml_payload.length > 2048) {
        // Do not leak details
        return res.status(400).json({ error: "Invalid payload." });
    }

    let data;
    try {
        // Safe YAML parsing: disable schema extensions, only allow plain objects
        data = yaml.load(yaml_payload, { schema: yaml.FAILSAFE_SCHEMA });
    } catch (err) {
        // Do not leak parsing errors
        return res.status(400).json({ error: "Malformed YAML." });
    }

    // Validate: "name" must be a string, max length 256, sanitize output
    if (
        !data ||
        typeof data !== "object" ||
        typeof data.name !== "string" ||
        data.name.length === 0 ||
        data.name.length > 256
    ) {
        return res.status(400).json({ error: "Missing or invalid 'name' field." });
    }

    // Sanitize: remove dangerous characters (basic example)
    const sanitizedName = data.name.replace(/[<>"'`\\]/g, "");

    // Return only the sanitized name
    res.json({ name: sanitizedName });
});
// ...existing code...

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
