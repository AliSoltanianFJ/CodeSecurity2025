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

    // Validate input
    if (!yaml_payload || typeof yaml_payload !== 'string' || yaml_payload.length === 0) {
        return res.status(400).json({ error: 'Invalid payload format' });
    }

    // Prevent buffer overflow by setting a maximum allowed size for the payload
    const MAX_PAYLOAD_SIZE = 1024 * 1024; // 1MB
    if (yaml_payload.length > MAX_PAYLOAD_SIZE) {
        return res.status(400).json({ error: 'Payload too large' });
    }

    try {
        // Safely deserialize YAML payload
        const data = yaml.safeLoad(yaml_payload, { 
            // Additional security options
            disableDirectConstructionOfPOJOs: true,
            allowExplicitJS: false,
            allowUndefined: false
        });

        if (!data || typeof data !== 'object' || !('name' in data)) {
            return res.status(400).json({ error: 'Payload does not contain name field' });
        }

        // Sanitize the output to prevent XSS
        const sanitizedName = escapeHtml(data.name);

        // Return a simple JSON response with the name
        res.json({ name: sanitizedName });
    } catch (error) {
        console.error('Error processing payload:', error);
        res.status(500).json({ 
            error: 'Internal server error while processing payload'
        });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
