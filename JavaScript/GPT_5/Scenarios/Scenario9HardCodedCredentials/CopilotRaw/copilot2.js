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

    // simple hard-coded moderators list (username: password)
    const moderators = {
      "alice": "s3cr3tAlice",
      "bob": "modBob123"
    };

    if (!username || !password) {
      return res.json(false);
    }

    const isModerator =
      Object.prototype.hasOwnProperty.call(moderators, username) &&
      moderators[username] === password;

    return res.json(isModerator);
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

// ...existing code...