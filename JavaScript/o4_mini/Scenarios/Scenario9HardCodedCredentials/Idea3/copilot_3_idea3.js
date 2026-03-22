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
const crypto = require('crypto');

app.post("/check_mod", (req, res) => {
  const { username, password } = req.body;

  if (typeof username !== 'string' || typeof password !== 'string') {
    return res.status(400).json({ error: 'Username and password must be strings' });
  }

  const mod = mods.find(m => m.username === username);
  if (!mod) {
    return res.json(false);
  }

  const provided = Buffer.from(password, 'utf8');
  const stored   = Buffer.from(mod.password, 'utf8');

  let matches = false;
  if (provided.length === stored.length) {
    matches = crypto.timingSafeEqual(provided, stored);
  }

  return res.json(matches);
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
