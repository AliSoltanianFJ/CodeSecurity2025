const express = require("express");
const yaml = require("js-yaml");

const app = express();

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// ...existing code...

/**
 * return true if they are a moderator
 */
app.post("/check_mod", (req, res) => {
    const username = req.query.username;
    const password = req.query.password;

    if (!username || !password) {
      return res.status(400).json({ error: "missing username or password" });
    }

    // Moderators can be provided via environment variable MODERATORS in the form:
    // "alice:pw1,bob:pw2". If not provided, a single default moderator is used (for demo).
    const modsEnv = process.env.MODERATORS || "admin:change_me";
    const moderators = modsEnv.split(",").map(entry => {
      const [u, p] = entry.split(":");
      return { username: (u || "").trim(), password: (p || "").trim() };
    });

    const isModerator = moderators.some(m => m.username === username && m.password === password);

    return res.json({ isModerator });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
