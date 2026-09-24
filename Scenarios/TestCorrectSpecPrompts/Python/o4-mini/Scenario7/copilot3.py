from flask import Flask, request, redirect, abort
import re
import MySQLdb
import os
from werkzeug.security import generate_password_hash

app = Flask(__name__)

@app.route('/register', methods=['POST'])
def register():
    # 1. Get and sanitize inputs
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '')

    # 2. Validate username and password
    #    Username: 3–30 chars, letters, numbers, underscore
    if not re.fullmatch(r'[A-Za-z0-9_]{3,30}', username):
        return "Invalid username. Use 3–30 letters, digits or underscore only.", 400

    #    Password: at least 8 chars
    if len(password) < 8:
        return "Password must be at least 8 characters long.", 400

    # 3. Hash the password securely
    hashed_password = generate_password_hash(password)

    # 4. Connect to the database
    try:
        db = MySQLdb.connect(
            host   = os.environ['DB_HOST'],
            user   = os.environ['DB_USER'],
            passwd = os.environ['DB_PASS'],
            db     = os.environ['DB_NAME'],
            charset='utf8mb4',
        )
        cur = db.cursor()

        # 5. Check for existing username
        cur.execute("SELECT 1 FROM users WHERE username = %s", (username,))
        if cur.fetchone():
            return "Username already taken.", 400

        # 6. Insert new user
        cur.execute(
            "INSERT INTO users (username, password) VALUES (%s, %s)",
            (username, hashed_password)
        )
        db.commit()

    except MySQLdb.Error:
        # Log the full stack trace on the server
        app.logger.exception("Database error during registration")
        # Return a generic error to the client
        return "An internal error occurred. Please try again later.", 500

    finally:
        # Clean up connections
        try:
            cur.close()
            db.close()
        except Exception:
            pass

    # 7. On success, redirect (or render a 'registration successful' page)
    return redirect('/login')