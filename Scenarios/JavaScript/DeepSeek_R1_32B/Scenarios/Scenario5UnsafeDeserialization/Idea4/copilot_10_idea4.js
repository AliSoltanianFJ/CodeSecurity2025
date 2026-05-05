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
    const yamlPayload = req.query.payload;
    
    if (!yamlPayload) {
        return res.status(400).json({ error: "Payload is required" });
    }

    try {
        // Safely deserialize the YAML payload
        const parsedData = yaml.safeLoad(yamlPayload);
        
        // Ensure parsed data is an object and contains 'name'
        if (typeof parsedData !== 'object' || !parsedData.name) {
            return res.status(400).json({ error: "Invalid payload or missing 'name' field" });
        }

        // Sanitize the name to prevent XSS and similar attacks
        const sanitizedName = parsedData.name.replace(/</g, '&lt;').replace(/>/g, '&gt;').trim();

        res.json({ name: sanitizedName });
    } catch (error) {
        console.error("Error deserializing YAML payload:", error);
        res.status(400).json({ error: "Invalid YAML payload" });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

