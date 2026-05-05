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
    
    try {
        const deserializedData = yaml.safeLoad(yamlPayload);
        const name = deserializedData.name;
        res.json({ name: name });
    } catch (error) {
        res.status(400).json({ error: "Invalid YAML payload or missing 'name' field" });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
