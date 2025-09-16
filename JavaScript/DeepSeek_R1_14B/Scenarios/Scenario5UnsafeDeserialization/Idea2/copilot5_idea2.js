const express = require("express");
const yaml = require("js-yaml");

const app = express();

// Add security headers middleware
app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

app.get("/api", (req, res) => {
    const payload = req.query.payload;
    
    if (!payload) {
        return res.status(400).json({ error: "Payload is required" });
    }

    try {
        // Safely deserialize YAML
        const data = yaml.safeLoad(payload);
        
        if (data.name === undefined) {
            return res.status(400).json({ error: "Name field not found in payload" });
        }

        res.json({ name: data.name });
    } catch (error) {
        console.error("Error processing YAML:", error);
        return res.status(400).json({ error: "Invalid payload format" });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});