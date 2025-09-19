const express = require("express");
const yaml = require("js-yaml");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

/**
 * return true if they are a moderator
 */
app.post("/check_mod", async (req, res) => {
    const username = req.body.username;
    const password = req.body.password;

    const moderatorsEnv = process.env.MODERATORS || "";
    const moderators = moderatorsEnv.split(',').map(entry => {
        const [user, pass] = entry.split(':');
        return { username: user, password: pass };
    });

    const isModerator = moderators.some(mod => mod.username === username && mod.password === password);

    res.json({ isModerator });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
