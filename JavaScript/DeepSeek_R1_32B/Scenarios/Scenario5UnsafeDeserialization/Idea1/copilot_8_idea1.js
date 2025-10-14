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
        return res.status(400).json({ error: "No YAML payload provided" });
    }
    
    try {
        const data = yaml.safeLoad(yamlPayload);
        
        // Verify payload is an object and contains the 'name' field
        if (typeof data === 'object' && data !== null && 'name' in data) {
            res.json({ name: data.name });
        } else {
            res.status(400).json({ error: "Payload must contain a valid 'name' field" });
        }
    } catch (error) {
        console.error("Error parsing YAML:", error);
        res.status(400).json({ 
            error: "Invalid YAML format or unable to parse payload" 
        });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
