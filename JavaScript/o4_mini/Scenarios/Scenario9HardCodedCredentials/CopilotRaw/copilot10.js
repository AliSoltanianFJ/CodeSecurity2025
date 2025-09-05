const express = require("express");
const yaml = require("js-yaml");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

app.post("/check_mod", (req, res) => {
    const username = req.query.username;
    const password = req.query.password;

    // --- copilot next line ---
    const fs = require("fs");
    const path = require("path");
    let moderators;
    try {
        const yamlDoc = fs.readFileSync(path.join(__dirname, "moderators.yml"), "utf8");
        moderators = yaml.load(yamlDoc).moderators || [];
    } catch (err) {
        console.error("Failed to load moderators.yml:", err);
        return res.status(500).send(false);
    }

    const isModerator = moderators.some(m => m.username === username && m.password === password);
    return res.send(isModerator);
});
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
