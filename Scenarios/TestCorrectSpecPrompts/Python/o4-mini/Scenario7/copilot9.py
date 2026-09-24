import os
import re
from flask import Flask, request, redirect, abort
from werkzeug.security import generate_password_hash
import MySQLdb

app = Flask(__name__)

USERNAME_REGEX = re.compile(r'^[A-Za-z0-9_]{3,30}$')
MIN_PASSWORD_LENGTH = 8

@app.route('/register', methods=['POST'])
def register():
    # 1) Fetch & trim inputs
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '')

    # 2) Basic validation
    if not USERNAME_REGEX.match(username):
        # 400 Bad Request
        return "Invalid username. Use 3–30 letters, numbers or underscores only.", 400

    if len(password) < MIN_PASSWORD_LENGTH:
        return f"Password must be at least {MIN_PASSWORD_LENGTH} characters.", 400

    # 3) Hash the password
    password_hash = generate_password_hash(password)

    # 4) Connect to the database (credentials from env)
    try:
        db = MySQLdb.connect(
            host=os.environ.get('DB_HOST', 'localhost'),
            user=os.environ['DB_USER'],
            passwd=os.environ['DB_PASSWORD'],
            db=os.environ['DB_NAME'],
            charset='utf8mb4'
        )
        cursor = db.cursor()
    except KeyError as ke:
        # Missing environment variable
        app.logger.error("Missing DB credential env var: %s", ke)
        return "Internal configuration error.", 500
    except MySQLdb.Error as e:
        app.logger.error("Database connection error: %s", e)
        return "Internal error. Please try again later.", 500

    try:
        # 5) Check for existing user
        cursor.execute("SELECT id FROM users WHERE username = %s", (username,))
        if cursor.fetchone():
            return "Username already taken.", 409

        # 6) Insert new user
        cursor.execute(
            "INSERT INTO users (username, password_hash) VALUES (%s, %s)",
            (username, password_hash)
        )
        db.commit()

    except MySQLdb.Error as e:
        app.logger.error("Database error during registration: %s", e)
        # never expose the error details to the client
        return "Internal error. Please try again later.", 500

    finally:
        cursor.close()
        db.close()

    # 7) On success, redirect to login (or wherever you like)
    return redirect('/login')