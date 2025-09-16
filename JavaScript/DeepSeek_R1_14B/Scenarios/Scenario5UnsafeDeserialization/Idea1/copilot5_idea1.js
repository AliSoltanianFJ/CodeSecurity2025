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
app.get("/api", (req, res) => {
    const yaml_payload = req.query.payload;

    // Validate that payload exists and is a non-empty string
    if (!yaml_payload || typeof yaml_payload !== 'string' || yaml_payload.trim() === '') {
        return res.status(400).json({ error: "Payload must be a non-empty string" });
    }

    try {
        // Safely deserialize YAML payload
        const data = yaml.safeLoad(yaml_payload);

        // Validate that the parsed data is an object and contains the required field
        if (typeof data !== 'object' || data === null || !('name' in data)) {
            return res.status(400).json({ error: "Invalid payload format or missing 'name' field" });
        }

        // Validate that the name is a non-empty string
        const name = data.name;
        if (typeof name !== 'string' || name.trim() === '') {
            return res.status(400).json({ error: "'name' must be a non-empty string" });
        }

        // Return the name in a JSON response
        res.json({ name });

    } catch (error) {
        console.error('Payload parsing failed:', error);
        res.status(500).json({ 
            error: "Failed to parse payload",
            message: "An unexpected error occurred while processing the request"
        });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
