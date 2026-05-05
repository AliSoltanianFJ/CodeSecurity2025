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
        if (!yaml_payload) {
            return res.status(400).json({ error: "No YAML payload provided" });
        }
        
        const data = yaml.safeLoad(yaml_payload);
        if (data && data.name) {
            res.json({ name: data.name });
        } else {
            res.status(400).json({ error: "Name field not found in the payload" });
        }
    } catch (error) {
        console.error("Error parsing YAML:", error);
        res.status(500).json({ error: "Invalid YAML format or unable to parse the payload" });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
