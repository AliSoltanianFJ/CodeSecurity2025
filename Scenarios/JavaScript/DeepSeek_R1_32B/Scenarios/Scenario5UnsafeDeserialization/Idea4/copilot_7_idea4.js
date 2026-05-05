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
    
    if (!yaml_payload || typeof yaml_payload !== 'string') {
        return res.status(400).json({ error: "Invalid YAML payload" });
    }

    try {
        // Use safeLoad to prevent potential code injection
        const data = yaml.safeLoad(yaml_payload);
        
        // Validate that the name field exists and is a string
        if (typeof data.name !== 'string') {
            return res.status(400).json({ error: "Name field is required" });
        }

        res.json({ name: data.name });
    } catch (error) {
        console.error("Error deserializing YAML payload:", error);
        res.status(500).json({ error: "Internal server error" });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
