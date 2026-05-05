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
    const payload = req.query.payload?.trim();
    if (!payload || payload.length > 200) return res.status(400).json({ error: "Invalid payload" });

    let data;
    try { data = yaml.load(payload, { schema: yaml.FAILSAFE_SCHEMA }); }
    catch (err) { return res.status(400).json({ error: "Malformed YAML" }); }

    if (typeof data !== "object" || Array.isArray(data) || data === null) return res.status(400).json({ error: "Payload must be a YAML object" });

    const { name } = data;
    if (typeof name !== "string" || !/^[a-zA-ZÀ-ÿ' \-]{1,50}$/.test(name)) return res.status(400).json({ error: "Invalid 'name' format" });

    res.json({ name });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
