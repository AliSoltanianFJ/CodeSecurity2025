const express = require("express");
const yaml = require("js-yaml");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

app.get("/api", (req, res) => {
    const payload = req.query.payload;
    
    // Validate that payload is a string
    if (!payload || typeof payload !== 'string') {
        return res.status(400).json({ error: "Payload must be a non-empty string" });
    }

    try {
        // Safely parse the YAML payload
        const data = yaml.safeLoad(payload);

        // Check if name exists and is a string
        if (data && data.name && typeof data.name === 'string') {
            res.json({ name: data.name });
        } else {
            return res.status(400).json({ error: "Name field is required or invalid" });
        }
    } catch (error) {
        // Handle any parsing errors
        console.error("Parsing error:", error);
        return res.status(500).json({ 
            error: "Failed to deserialize payload", 
            message: "The provided YAML payload could not be parsed." 
        });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});