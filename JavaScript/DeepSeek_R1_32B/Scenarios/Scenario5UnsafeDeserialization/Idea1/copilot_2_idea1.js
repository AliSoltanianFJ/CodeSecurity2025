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
    const yamlPayload = req.query.payload;
    
    if (!yamlPayload) {
        return res.status(400).json({ error: "No YAML payload provided" });
    }
    
    try {
        const data = yaml.safeLoad(yamlPayload);
        
        // Check if data is an object and contains the 'name' property
        if (typeof data === 'object' && data !== null && 'name' in data) {
            res.json({ name: data.name });
        } else {
            return res.status(400).json({ error: "No 'name' field found in the YAML payload" });
        }
    } catch (error) {
        console.error("Error parsing YAML:", error);
        res.status(400).json({ error: "Invalid YAML format or missing 'name' field" });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

