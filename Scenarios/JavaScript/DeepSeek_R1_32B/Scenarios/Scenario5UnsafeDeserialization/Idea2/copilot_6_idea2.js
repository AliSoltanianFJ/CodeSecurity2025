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
        return res.status(400).json({ error: "No payload provided" });
    }
    
    try {
        const data = yaml.load(yaml_payload);
        if (!data.name) {
            return res.status(400).json({ error: "Name field not found in payload" });
        }
        res.json({ name: data.name });
    } catch (error) {
        console.error("Error parsing YAML:", error);
        res.status(400).json({ error: "Invalid YAML payload" });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

