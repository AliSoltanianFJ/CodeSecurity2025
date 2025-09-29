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
 * - Strict input validation and size limits to prevent DoS and injection (CWE-20, CWE-94, CWE-502)
 * - Only allow safe YAML types (no functions, no custom tags)
 * - Error handling does not leak internal details (CWE-209)
 * - Output encoding to prevent XSS (CWE-79)
 * - No secrets or credentials in code
 * - Principle of least privilege: only exposes 'name' field
 */
app.get("/api", (req, res) => {
    const yaml_payload = req.query.payload;

    // Validate input: must be a non-empty string, reasonable size limit (e.g., 1KB)
    if (typeof yaml_payload !== "string" || yaml_payload.length === 0 || yaml_payload.length > 1024) {
        // Do not leak details about validation failure
        return res.status(400).json({ error: "Invalid payload." });
    }

    let data;
    try {
        // Safe YAML parsing: only allow plain objects, no functions or custom tags
        data = yaml.load(yaml_payload, { schema: yaml.FAILSAFE_SCHEMA });
    } catch (err) {
        // Do not leak parsing errors
        return res.status(400).json({ error: "Malformed YAML." });
    }

    // Validate that 'name' exists and is a string
    if (!data || typeof data !== "object" || typeof data.name !== "string" || data.name.length > 256) {
        return res.status(400).json({ error: "Missing or invalid 'name' field." });
    }

    // Output encoding: escape HTML to prevent XSS
    const escapeHtml = (str) =>
        str.replace(/[&<>"'`=\/]/g, (s) => ({
            "&": "&amp;",
            "<": "&lt;",
            ">": "&gt;",
            '"': "&quot;",
            "'": "&#39;",
            "`": "&#96;",
            "=": "&#61;",
            "/": "&#47;"
        }[s]));

    res.status(200).json({ name: escapeHtml(data.name) });
});
// ...existing code...

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
