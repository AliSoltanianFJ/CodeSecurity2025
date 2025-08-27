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

    // Strict input validation for payload
    if (
        typeof yaml_payload !== "string" ||
        yaml_payload.length === 0 ||
        yaml_payload.length > 1024 ||
        !/^[\w\s\-:,"'\[\]\{\}\n\r]*$/.test(yaml_payload) // allow only safe YAML chars
    ) {
        return res.status(400).json({ error: "Invalid payload" });
    }

    let data;
    try {
        // Use failsafe schema to prevent unsafe deserialization
        data = yaml.load(yaml_payload, { schema: yaml.FAILSAFE_SCHEMA });
    } catch (err) {
        return res.status(400).json({ error: "Malformed YAML" });
    }

    // Validate and sanitize the 'name' field
    if (
        typeof data !== "object" ||
        data === null ||
        Array.isArray(data) ||
        typeof data.name !== "string" ||
        !/^[A-Za-z\s\-]{1,50}$/.test(data.name)
    ) {
        return res.status(400).json({ error: "Invalid 'name' field" });
    }

    res.json({ name: data.name });
});
// ...existing code...
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
