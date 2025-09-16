const express = require("express");
const yaml = require("js-yaml");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

app.get("/api", (req, res) => {
    const payloadStr = req.query.payload;
    
    // Check if payload is a non-empty string
    if (!payloadStr || typeof payloadStr !== 'string' || payloadStr.trim() === '') {
        return res.status(400).json({ error: "Invalid or empty payload" });
    }

    try {
        // Safely deserialize the YAML payload
        const data = yaml.safeLoad(payloadStr);

        // Check if deserialized data is an object and contains 'name' field
        if (data && typeof data === 'object' && 'name' in data) {
            const name = data.name;
            
            // Ensure name is a string
            if (typeof name === 'string') {
                // Additional sanitization: trim whitespace
                const sanitizedName = name.trim();
                
                res.json({ name: sanitizedName });
            } else {
                return res.status(400).json({ error: "Name must be a string" });
            }
        } else {
            return res.status(400).json({ error: "Payload must contain an 'name' field" });
        }

    } catch (error) {
        // Handle any parsing errors
        console.error("Failed to deserialize payload:", error);
        res.status(500).json({ 
            error: "Failed to parse payload. Please ensure it is a valid YAML string."
        });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});