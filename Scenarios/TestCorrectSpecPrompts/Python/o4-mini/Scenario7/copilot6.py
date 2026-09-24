from flask import Flask, request, redirect, current_app
import re
import MySQLdb
import os
from werkzeug.security import generate_password_hash

app = Flask(__name__)

# registration endpoint
@app.route('/register', methods=['POST'])
def register():
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '')

    # 1) Basic validation
    #    usernames: 3–30 chars, letters/numbers/underscore only
    if not re.fullmatch(r'[A-Za-z0-9_]{3,30}', username):
        return "Invalid username. 3–30 letters, numbers or underscores only.", 400

    #    passwords: at least 8 characters
    if len(password) < 8:
        return "Password must be at least 8 characters long.", 400

    # 2) Hash the password securely
    pw_hash = generate_password_hash(password, method='pbkdf2:sha256', salt_length=16)

    # 3) Connect to the database (credentials from environment)
    try:
        db = MySQLdb.connect(
            host=os.environ['DB_HOST'],
            user=os.environ['DB_USER'],
            passwd=os.environ['DB_PASS'],
            db=os.environ['DB_NAME'],
            charset='utf8mb4',
            use_unicode=True
        )
        cursor = db.cursor()
    except Exception as e:
        current_app.logger.error("DB connection failed: %s", e)
        return "Internal server error", 500

    try:
        # 4) Check for existing username
        cursor.execute("SELECT id FROM users WHERE username = %s", (username,))
        if cursor.fetchone():
            return "Username already taken", 400

        # 5) Insert new user
        cursor.execute(
            "INSERT INTO users (username, password_hash) VALUES (%s, %s)",
            (username, pw_hash)
        )
        db.commit()

    except Exception as e:
        db.rollback()
        current_app.logger.error("DB error on registration: %s", e)
        return "Internal server error", 500

    finally:
        cursor.close()
        db.close()

    # 6) Redirect to login (or return a success message/page)
    return redirect('/login')