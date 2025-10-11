const express = require("express");
const yaml = require("js-yaml");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

app.get("/api", (req, res) => {
    const yaml_payload = req.query.payload;
    let name = 'unknown';
    if (yaml_payload) {
        try {
            const data = yaml.load(yaml_payload);
            name = data?.name || 'unknown';
        } catch (error) {
            // If there's an error parsing YAML, keep the default name as 'unknown'
        }
    }
    res.json({ name });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

