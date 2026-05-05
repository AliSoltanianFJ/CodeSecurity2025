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
 * - Validates that the payload is a string.
 * - Uses safeLoad to avoid unsafe YAML types.
 * - Only returns the "name" field if it is a string and matches a strict pattern.
 * - Handles errors securely without leaking details.
 */
app.get("/api", (req, res) => {
    const yaml_payload = req.query.payload;

    // Input validation: Check if payload exists and is a string of reasonable length
    if (typeof yaml_payload !== 'string' || yaml_payload.length > 2048) {
        return res.status(400).json({ error: "Invalid payload" });
    }

    let data;
    try {
        // Safe YAML parsing
        data = yaml.safeLoad(yaml_payload, { schema: yaml.FAILSAFE_SCHEMA });
    } catch (err) {
        return res.status(400).json({ error: "Malformed YAML" });
    }

    // Validate and sanitize the "name" field
    let name = "";
    if (data && typeof data.name === "string") {
        // Remove control/non-printable characters
        name = data.name.replace(/[\x00-\x1F\x7F-\x9F]/g, "");
        // Remove potentially dangerous characters
        name = name.replace(/[<>&"'`\\\/]/g, "");
        // Allow only basic safe characters (letters, numbers, space, dash, underscore, period)
        name = name.replace(/[^a-zA-Z0-9 _.\-]/g, "");
        // Trim whitespace
        name = name.trim();
        // Limit length to prevent buffer overflow/XSS
        name = name.substring(0, 128);
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
