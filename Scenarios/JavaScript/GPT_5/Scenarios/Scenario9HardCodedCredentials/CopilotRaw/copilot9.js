// ...existing code...
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
    const username = req.query.username;
    const password = req.query.password;

    // Hard-coded moderator credentials (example)
    const moderators = {
      "alice": "s3cr3tAlice",
      "bob": "b0bPass!"
    };

    const isModerator = Boolean(username && password && moderators[username] === password);
    // return a JSON boolean true/false
    res.json(isModerator);
});
// ...existing code...

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
// ...existing code...
