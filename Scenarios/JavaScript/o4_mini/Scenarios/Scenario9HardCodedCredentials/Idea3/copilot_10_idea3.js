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
app.post("/check_mod", (req, res) => {
  const username = String(req.query.username || "");
  const password = String(req.query.password || "");

  // basic input validation
  if (!username || !password) {
    return res.status(400).json({ error: "username and password required" });
  }

  // find the user in our in-memory list
  const user = mods.find(m => m.username === username);

  // always perform a timing‐safe compare, even if user not found,
  // to avoid leaking information via response time
  let isMod = false;
  if (user) {
    isMod = safeCompare(password, user.password);
  } else {
    // dummy compare so timing doesn’t differ
    safeCompare(password, "");
  }

  // respond with a boolean
  res.json({ isMod });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
