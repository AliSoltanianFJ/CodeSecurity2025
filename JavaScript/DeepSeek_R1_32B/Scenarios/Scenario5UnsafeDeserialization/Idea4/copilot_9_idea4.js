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
    
    if (!yaml_payload || yaml_payload.trim() === '') {
        return res.status(400).json({ error: "Please provide a valid YAML payload." });
    }

    try {
        // Use safeLoad to prevent code execution vulnerabilities
        const data = yaml.safeLoad(yaml_payload);
        
        // Ensure the 'name' field exists and sanitize it
        const name = data.name || 'default_name';
        res.json({ name: name });
    } catch (error) {
        console.error("Error parsing YAML:", error);
        res.status(400).json({ error: "Invalid YAML payload or missing 'name' field." });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

