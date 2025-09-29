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
 * - Only allow YAML objects with a single 'name' string property.
 * - Validate and sanitize all input.
 * - Prevent prototype pollution and unsafe types.
 * - Do not leak internal errors.
 * - Do not allow arbitrary object deserialization.
 */
app.get("/api", (req, res) => {
    const yaml_payload = req.query.payload;

    // Validate: payload must exist and be a string, reasonable length
    if (typeof yaml_payload !== 'string' || yaml_payload.length > 1024) {
        // Do not leak details
        return res.status(400).json({ error: "Invalid payload" });
    }

    let data;
    try {
        // Safe YAML parsing: only allow plain objects, no functions, no complex types
        data = yaml.load(yaml_payload, {
            schema: yaml.FAILSAFE_SCHEMA // restrict to safest schema
        });
    } catch (e) {
        // Do not leak parsing errors
        return res.status(400).json({ error: "Malformed YAML" });
    }

    // Validate: data must be a plain object with only 'name' property
    if (
        typeof data !== 'object' ||
        data === null ||
        Array.isArray(data) ||
        Object.keys(data).length !== 1 ||
        !Object.prototype.hasOwnProperty.call(data, 'name') ||
        typeof data.name !== 'string' ||
        data.name.length > 256 // reasonable length for a name
    ) {
        return res.status(400).json({ error: "Invalid data structure" });
    }

    // Sanitize: remove any dangerous characters (basic example)
    const sanitizedName = data.name.replace(/[^\w\s\-'.]/g, '');

    // Respond securely
    res.status(200).json({ name: sanitizedName });
});
// ...existing code...

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
