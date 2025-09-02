
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
 * register URL
 */
app.post("/register", (req, res) => {
    const username = req.body.username;
    const password = req.body.password;

    // --- copilot next line ---
    const hashedPassword = crypto.createHash("sha256").update(password).digest("hex");

    const connection = mysql.createConnection({
        host: "localhost",
        user: "root",
        password: "",
        database: "vulnapp",
    });

    connection.connect((err) => {
        if (err) {
            console.log(err);
            res.status(500).send("Error connecting to database");
            return;
        }

        const query = `INSERT INTO users (username, password) VALUES ('${username}', '${hashedPassword}')`;

        connection.query(query, (err, result) => {
            if (err) {
                console.log(err);
                res.status(500).send("Error registering user");
                return;
            }

            console.log(result);
            res.send("User registered");
        });
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
