const express = require("express");
const mysql   = require("mysql");
const crypto  = require("crypto");
const session = require("express-session");

const app = express();
app.use(express.urlencoded({ extended: true }));


app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

// create a MySQL connection pool
const db = mysql.createPool({
  host     : process.env.DB_HOST,
  user     : process.env.DB_USER,
  password : process.env.DB_PASSWORD,
  database : process.env.DB_NAME,
  connectionLimit : 10
});

/**
 * Log in the user if the username and password are correct
 */
app.post("/login", (req, res) => {
  const username = req.body.username;
  const password = req.body.password;
  
  if (!username || !password) {
    // avoid telling user which field was missing
    return res.status(400).send("Username and password required");
  }
  
  // 1) Fetch the user's salt and hashed password from the database
  const sql = "SELECT id, password_hash, salt FROM users WHERE username = ?";
  db.query(sql, [username], (err, results) => {
    if (err) {
      console.error("DB error on login:", err);
      return res.status(500).send("Internal server error");
    }
    
    if (results.length !== 1) {
      // user not found, but don't reveal that
      return res.status(401).send("Invalid username or password");
    }
    
    const { id: userId, password_hash: storedHashHex, salt: saltHex } = results[0];
    const saltBuf       = Buffer.from(saltHex, 'hex');
    const storedHashBuf = Buffer.from(storedHashHex, 'hex');
    
    // 2) Re-derive the key from the submitted password & the stored salt
    const iterations = 100000;
    const keyLen     = 64;
    const digest     = 'sha512';
    
    crypto.pbkdf2(password, saltBuf, iterations, keyLen, digest, (err, derivedKey) => {
      if (err) {
        console.error("PBKDF2 error on login:", err);
        return res.status(500).send("Internal server error");
      }
      
      // 3) Constant-time compare to prevent timing attacks
      if (derivedKey.length !== storedHashBuf.length ||
          !crypto.timingSafeEqual(derivedKey, storedHashBuf)) {
        return res.status(401).send("Invalid username or password");
      }
      
      // 4) Credentials are valid – establish a session
      req.session.regenerate(err => {
        if (err) {
          console.error("Session regeneration error:", err);
          return res.status(500).send("Internal server error");
        }
        
        req.session.userId = userId;
        req.session.username = username;
        
        // you can redirect or send JSON
        res.send("Login successful");
      });
    });
  });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});