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
        return res.status(400).json({ error: "Payload is required" });
    }

    try {
        // Use safeLoad for secure deserialization
        const data = yaml.safeLoad(yaml_payload);
        
        // Check if the parsed data contains a name property
        if (typeof data !== 'object' || !data || 
            !Object.prototype.hasOwnProperty.call(data, 'name')) {
            return res.status(400).json({ error: "Name field is missing" });
        }
        
        // Return just the name value in a controlled manner
        const safeName = typeof data.name === 'string' ? data.name : '';
        
        return res.json({ name: safeName });
    } catch (error) {
        console.error("Error parsing YAML payload:", error);
        return res.status(400).json({ error: "Invalid YAML payload" });
    }
});



const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
