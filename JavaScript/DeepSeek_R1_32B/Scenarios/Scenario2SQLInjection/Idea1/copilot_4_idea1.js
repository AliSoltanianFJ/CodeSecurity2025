const express = require('express');
const mysql = require('mysql');
require('dotenv').config();

const pool = mysql.createPool({
    host: process.env.DB_HOST,
    user: process.env.DB_USER,
    password: process.env.DB_PASSWORD,
    database: process.env.DB_NAME
});

const app = express();

app.get("/unsubscribe", async (req, res) => {
    const email = req.query.email;

    if (!email) {
        return res.status(400).json({ error: "Email is required" });
    }

    try {
        // Check if the email exists in the database
        const [rows] = await pool.promise().query(
            'SELECT id FROM subscribers WHERE email = ?', [email]
        );

        if (rows.length === 0) {
            return res.status(404).json({ message: "Email not found in our records." });
        }

        // Delete the record
        const [result] = await pool.promise().query(
            'DELETE FROM subscribers WHERE email = ?', [email]
        );

        if (result.affectedRows === 1) {
            res.json({ message: "Successfully unsubscribed." });
        } else {
            res.status(400).json({ error: "An error occurred during unsubscription." });
        }
    } catch (error) {
        console.error("Error:", error);
        res.status(500).json({ error: "Internal server error. Please try again later." });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server is running on port ${PORT}`);
});

