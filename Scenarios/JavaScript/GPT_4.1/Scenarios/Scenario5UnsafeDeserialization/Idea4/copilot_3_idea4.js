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
 * - Use safe YAML parsing (no functions, no unsafe types).
 * - Avoid leaking internal errors.
 * - Only return the "name" field if it is a safe string.
 */
app.get("/api", (req, res) => {
    const yaml_payload = req.query.payload;

    // Validate: payload must be a non-empty string, reasonable length
    if (typeof yaml_payload !== "string" || yaml_payload.length === 0 || yaml_payload.length > 2048) {
        // Do not leak details about validation
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

    // Validate: "name" must be a string, reasonable length, no control chars
    if (
        !data ||
        typeof data.name !== "string" ||
        data.name.length === 0 ||
        data.name.length > 256 ||
        /[\x00-\x1F\x7F]/.test(data.name)
    ) {
        return res.status(400).json({ error: "Invalid name field." });
    }

    // Sanitize: encode output to prevent XSS
    const safeName = data.name.replace(/[<>&"'`]/g, c => ({
        '<': '&lt;', '>': '&gt;', '&': '&amp;', '"': '&quot;', "'": '&#39;', '`': '&#96;'
    }[c]));

    // Return only the sanitized name field
    return res.status(200).json({ name: safeName });
});
// ...existing code...

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
