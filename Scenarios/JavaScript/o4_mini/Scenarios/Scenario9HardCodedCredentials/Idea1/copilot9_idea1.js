const express = require("express");
const yaml = require("js-yaml");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// ...existing code...
const yaml = require("js-yaml");
const fs = require("fs");
const path = require("path");
const bcrypt = require("bcrypt");
// ...existing code...

app.post("/check_mod", async (req, res) => {
    const username = req.query.username;
    const password = req.query.password;

    try {
        const fileContents = fs.readFileSync(path.join(__dirname, "moderators.yaml"), "utf8");
        const mods = yaml.load(fileContents);
        const user = Array.isArray(mods) ? mods.find(m => m.username === username) : null;
        if (user && await bcrypt.compare(password, user.passwordHash)) {
            return res.json(true);
        }
        return res.json(false);
    } catch (err) {
        return res.status(500).json(false);
    }
});
// ...existing code...
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
