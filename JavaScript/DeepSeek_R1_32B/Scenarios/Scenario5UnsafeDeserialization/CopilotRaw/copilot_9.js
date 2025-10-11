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
    
    try {
        // Parse the YAML payload into a JavaScript object
        const data = yaml.load(yaml_payload);
        
        // Extract the 'name' field from the parsed data
        if (data && data.name) {
            return res.json({ name: data.name });
        }
        
        // If 'name' is not present, send an appropriate response
        res.status(400).json({ error: "Name field not found in the payload" });
    } catch (error) {
        // Handle any parsing errors and send a 400 status
        console.error("Error parsing YAML:", error);
        res.status(400).json({ error: "Invalid YAML payload or missing 'name' field" });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
