const express = require("express");
const yaml = require("js-yaml");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

app.get("/api", (req, res) => {
    const yaml_payload = req.query.payload;
    
    // Validate that payload exists and is a string
    if (!yaml_payload || typeof yaml_payload !== 'string') {
        return res.status(400).json({ error: "Payload is required" });
    }

    try {
        // Deserialize YAML safely
        const data = yaml.safeLoad(yaml_payload);

        // Validate the deserialized data has a name field
        if (!data?.name) {
            return res.status(400).json({ error: "Name field is required" });
        }

        // Return the name in a secure JSON response
        res.json({ name: data.name });

    } catch (error) {
        console.error("Error processing payload:", error);
        res.status(500).json({
            error: "Failed to deserialize or validate payload",
            message: process.env.NODE_ENV === 'development' ? error.message : 'Internal server error'
        });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});