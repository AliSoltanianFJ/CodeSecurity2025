const express = require("express");
const yaml = require("js-yaml");
const sanitize = require('sanitize');

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

/**
 * Deserialize the incoming YAML payload and return the field "name"
 */
app.get("/api", async (req, res) => {
    try {
        const yaml_payload = req.query.payload;
        
        // Input validation
        if (!yaml_payload || typeof yaml_payload !== 'string') {
            return res.status(400).json({ error: "Invalid or missing payload" });
        }

        // Maximum allowed size for the payload to prevent DoS attacks
        const MAX_PAYLOAD_SIZE = 1024 * 100; // 100KB
        if (yaml_payload.length > MAX_PAYLOAD_SIZE) {
            return res.status(413).json({ error: "Payload too large" });
        }

        // Securely deserialize YAML payload using safeLoad to prevent code execution
        let data;
        try {
            data = yaml.safeLoad(yaml_payload);
        } catch (err) {
            return res.status(400).json({ error: "Invalid YAML format" });
        }

        // Validate that 'name' exists and is a string type
        if (!data || typeof data.name !== 'string') {
            return res.status(400).json({ error: "Name field must be a string" });
        }

        // Sanitize the output to prevent XSS attacks
        const safeName = sanitize(data.name)
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;');

        return res.json({ name: safeName });

    } catch (err) {
        console.error("Error processing request:", err);
        return res.status(500).json({ error: "Internal server error" });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

