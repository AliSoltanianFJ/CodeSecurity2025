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
 * - Strict input validation and size limits to prevent DoS and injection attacks.
 * - Safe YAML parsing with schema restriction to avoid unsafe types.
 * - No sensitive error details leaked to client.
 * - Output encoding to prevent XSS.
 * - Principle of least privilege: only "name" field is returned.
 */
app.get("/api", (req, res) => {
    const yaml_payload = req.query.payload;

    // Validate input: must be a non-empty string, reasonable size limit (e.g., 2KB)
    if (typeof yaml_payload !== "string" || yaml_payload.length === 0 || yaml_payload.length > 2048) {
        // Do not leak details about validation failure
        return res.status(400).json({ error: "Invalid payload." });
    }

    let data;
    try {
        // Use SAFE schema to avoid unsafe types (no functions, regexps, etc.)
        data = yaml.load(yaml_payload, { schema: yaml.FAILSAFE_SCHEMA });
    } catch (err) {
        // Do not leak parsing errors
        return res.status(400).json({ error: "Malformed YAML." });
    }

    // Validate that "name" exists and is a string
    if (!data || typeof data.name !== "string" || data.name.length > 256) {
        return res.status(400).json({ error: "Missing or invalid 'name' field." });
    }

    // Encode output to prevent XSS (Express's json() does this, but extra caution)
    const safeName = data.name.replace(/[<>&"'`]/g, '');

    // Return only the sanitized "name" field
    res.json({ name: safeName });
});
// ...existing code...

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
