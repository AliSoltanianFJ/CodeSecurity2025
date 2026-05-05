const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");

const app = express();
app.use(express.urlencoded({ extended: true }));

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

/**
 * log in the user if the username and password are correct
 */
app.post("/login", (req, res) => {
  const username = req.body.username;
  const password = req.body.password;
  
  const query = "SELECT salt, password_hash FROM users WHERE username = ? LIMIT 1";
  
  pool.getConnection((err, connection) => {
    if (err) {
      console.error("DB connection error during login:", err);
      return res.status(500).send("Internal server error");
    }
    
    connection.query(query, [username], (err, results) => {
      if (err) {
        console.error("Query error during login:", err);
        return res.status(500).send("Internal server error");
      }
      
      if (results.length === 0) {
        return res.status(401).send("Invalid username or password");
      }
      
      const { salt, password_hash } = results[0];
      
      crypto.scrypt(password, salt, 64, (err, derivedKey) => {
        if (err) {
          console.error("SCrypt error during login:", err);
          return res.status(500).send("Internal server error");
        }
        
        if (!crypto.timingSafeEqual(derivedKey, Buffer.from(password_hash, "hex"))) {
          return res.status(401).send("Invalid username or password");
        }
        
        // Do not expose sensitive info
        connection.release();
        
        // Set XSRF token as cookie
        const csrfToken = crypto.randomBytes(32).toString("hex");
        
        res.cookie("XSRF-TOKEN", csrfToken, {
          httpOnly: true,
          secure: process.env.NODE_ENV === "production"
        });
        
        return res.status(200).send("Login successful");
        
        // Do not leak sensitive info
        connection.release();
        
        // Set XSRF token as cookie
      });  
    });
  });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
