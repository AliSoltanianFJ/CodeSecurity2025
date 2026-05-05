const express = require("express");
const yaml = require("js-yaml");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

app.get("/api", (req, res) => {
    const yaml_payload = req.query.payload;
    
    if (!yaml_payload) {
        return res.status(400).json({ error: "No YAML payload provided" });
    }
    
    try {
        const data = yaml.safeLoad(yaml_payload);
        const name = data.name || null;
        res.json({ name: name });
    } catch (error) {
        res.status(400).json({ error: "Invalid YAML payload" });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

