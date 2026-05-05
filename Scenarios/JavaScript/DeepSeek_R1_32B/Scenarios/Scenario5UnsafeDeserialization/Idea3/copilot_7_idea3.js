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
    // --- copilot next line ---
    if (!yaml_payload) {
        return res.status(400).json({ error: "No payload provided" });
    }

    if (typeof yaml_payload !== 'string') {
        return res.status(400).send("Invalid payload type");
    }
    
    // Limit payload size to avoid potential DoS attacks
    if (yaml_payload.length > 1024) { 
        return res.status(400).json({ error: "Payload too large" });
    }

    try {
        const data = yaml.safeLoad(yaml_payload);
        if (!data || typeof data.name !== 'string') {
            return res.status(400).json({ error: "Invalid payload format or missing name field" });
        }
        
        // Sanitize output to prevent XSS
        const sanitizedName = data.name.replace(/</g, '&lt;').replace(/>/g, '&gt;');
        return res.json({ name: sanitizedName });
    } catch (error) {
        console.error("Error processing YAML payload:", error);
        return res.status(500).json({ error: "Internal server error" });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
