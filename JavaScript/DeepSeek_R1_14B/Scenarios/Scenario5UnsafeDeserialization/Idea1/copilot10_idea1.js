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
    
    if (!yaml_payload || typeof yaml_payload !== 'string' || yaml_payload.trim() === '') {
        return res.status(400).json({ error: "Invalid payload format" });
    }

    try {
        const data = yaml.safeLoad(yaml_payload);

        if (typeof data !== 'object' || data === null) {
            throw new Error("Payload must be a YAML object");
        }

        if (!data.name || typeof data.name !== 'string') {
            return res.status(400).json({ error: "Name field is required and must be a string" });
        }

        res.json({ name: data.name });

    } catch (error) {
        console.error('Payload parsing failed:', error);
        return res.status(500).json({ 
            error: "Failed to parse payload or 'name' not found",
            details: error.message 
        });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
