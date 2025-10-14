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
    
    if (!yaml_payload) {
        return res.status(400).json({ error: "No YAML payload provided" });
    }
    
    try {
        const data = yaml.safeLoad(yaml_payload);
        
        // Ensure the parsed data is an object and not null
        if (typeof data === 'object' && data !== null) {
            const name = data.name;
            
            // Check if 'name' exists and is a string
            if (typeof name === 'string') {
                res.json({ name });
            } else {
                return res.status(400).json({ error: "The 'name' field must be a string" });
            }
        } else {
            return res.status(400).json({ error: "Parsed data is not an object or null" });
        }
    } catch (error) {
        console.error("Error parsing YAML:", error);
        res.status(400).json({ error: "Invalid YAML payload" });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

