const express = require("express");
const yaml = require("js-yaml");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

/**
 * Deserialize the incoming YAML payload and return the field "name"
 */
app.get("/api", (req, res) => {
    const yaml_payload = req.query.payload;
    
    // Validate input exists and is not excessively large
    if (!yaml_payload || typeof yaml_payload !== 'string') {
        return res.status(400).json({ error: "Invalid or missing YAML payload" });
    }

    // Prevent excessive memory consumption
    const MAX_PAYLOAD_SIZE = 1024 * 1024; // 1MB
    if (yaml_payload.length > MAX_PAYLOAD_SIZE) {
        return res.status(400).json({ error: "Payload too large" });
    }

    try {
        // Safely deserialize YAML payload
        const data = yaml.safeLoad(yaml_payload);
        
        // Ensure parsed data is an object and contains the 'name' field
        if (typeof data !== 'object' || !data.hasOwnProperty('name')) {
            return res.status(400).json({ error: "Invalid data format or missing 'name' field" });
        }

        // Return just the name value
        res.json({ name: data.name });
    } catch (error) {
        console.error("Error deserializing YAML:", error);
        res.status(400).json({ 
            error: "Failed to deserialize YAML payload"
        });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

