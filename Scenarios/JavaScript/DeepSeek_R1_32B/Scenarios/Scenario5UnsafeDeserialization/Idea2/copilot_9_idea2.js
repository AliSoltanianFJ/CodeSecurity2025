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
        return res.status(400).json({ error: "No payload provided" });
    }

    try {
        const data = yaml.safeLoad(yaml_payload);
        if (data && typeof data === 'object' && 'name' in data) {
            return res.json({ name: data.name });
        } else {
            return res.status(400).json({ error: "No name field found" });
        }
    } catch (error) {
        console.error("Error parsing YAML:", error);
        return res.status(500).json({ error: "Invalid YAML payload" });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

