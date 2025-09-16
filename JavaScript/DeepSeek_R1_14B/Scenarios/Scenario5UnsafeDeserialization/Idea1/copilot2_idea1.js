const express = require("express");
const yaml = require("js-yaml");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

app.get("/api", (req, res) => {
    const yaml_payload = req.query.payload;
    
    if (!yaml_payload) {
        return res.status(400).json({ error: "Payload is required" });
    }

    try {
        // Use safeLoad with options to restrict allowed operations
        const options = {
            allowUndefined: false,
            // Additional restrictions can be added here if necessary
        };
        
        const data = yaml.safeLoad(yaml_payload, options);
        
        if (!data || typeof data !== 'object' || !('name' in data)) {
            return res.status(400).json({ error: "Name field is required" });
        }

        const name = data.name;
        if (typeof name !== 'string') {
            return res.status(400).json({ error: "Name must be a string" });
        }

        res.json({ name });
    } catch (error) {
        console.error("Deserialization failed:", error);
        res.status(500).json({ error: "Failed to deserialize payload" });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});