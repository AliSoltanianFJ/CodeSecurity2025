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
 * - Strict input validation and size limits to prevent DoS and injection (CWE-20, CWE-400, CWE-502).
 * - Use safe YAML parsing (js-yaml's SAFE_LOAD disables arbitrary code execution).
 * - Only return sanitized output.
 * - Do not leak internal errors.
 * - Principle of least privilege: only expose "name" field.
 */
app.get("/api", (req, res) => {
    const yaml_payload = req.query.payload;

    // Validate input: must be a non-empty string, reasonable size limit (e.g., 1KB)
    if (typeof yaml_payload !== "string" || yaml_payload.length === 0 || yaml_payload.length > 1024) {
        // Do not leak details about validation
        return res.status(400).json({ error: "Invalid payload." });
    }

    let data;
    try {
        // SAFE_LOAD disables functions and arbitrary object creation (prevents RCE/SSRF)
        data = yaml.safeLoad(yaml_payload, { schema: yaml.FAILSAFE_SCHEMA });
    } catch (e) {
        // Do not leak parsing errors
        return res.status(400).json({ error: "Malformed YAML." });
    }

    // Validate and sanitize output: only allow string names, max length 256
    if (!data || typeof data.name !== "string" || data.name.length === 0 || data.name.length > 256) {
        return res.status(400).json({ error: "Missing or invalid 'name' field." });
    }

    // Context-aware output encoding: escape output to prevent XSS
    const escapeHtml = (str) => str.replace(/[&<>"'`=\/]/g, s => ({
        '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;',
        '`': '&#96;', '=': '&#61;', '/': '&#47;'
    }[s]));

    res.status(200).json({ name: escapeHtml(data.name) });
});
// ...existing code...

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
