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
        
        if (typeof data === 'object' && data !== null && 'name' in data) {
            res.json({ name: data.name });
        } else {
            res.status(400).json({ error: "No 'name' field found in the payload" });
        }
    } catch (error) {
        res.status(400).json({ error: "Invalid YAML format or unexpected data type" });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

