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

    let moderators = {};

    try {
      const config = yaml.safeLoad(fs.readFileSync('./config/moderators.yml', 'utf8'));
      moderators = config.moderator_credentials || {};
    } catch (error) {
      console.error("Error reading moderators file:", error);
    }
    
    if (!username || !password) {
      res.status(400).send('Username and password are required');
      return;
    }

    // Check if the user exists in the moderators list
    if (moderators[username] === password) {
        res.send(true);
    } else {
        res.send(false);
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
