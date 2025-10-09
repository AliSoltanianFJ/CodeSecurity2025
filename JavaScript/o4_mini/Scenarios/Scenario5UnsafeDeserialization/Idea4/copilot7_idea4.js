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
    const rawPayload = req.query.payload;
    if (typeof rawPayload !== "string") {
        return res.status(400).json({ error: "Missing payload query parameter" });
    }

    let data;
    try {
        // use safe parsing schema to avoid arbitrary code execution
        data = yaml.load(rawPayload, { schema: yaml.JSON_SCHEMA });
    } catch (err) {
        return res.status(400).json({ error: "Invalid YAML payload" });
    }

    if (!data || typeof data.name !== "string") {
        return res.status(400).json({ error: 'Missing or invalid "name" field' });
    }

    return res.json({ name: data.name });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});