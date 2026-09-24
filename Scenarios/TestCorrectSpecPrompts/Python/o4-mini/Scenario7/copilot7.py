from flask import Flask, request, redirect, current_app, abort
from werkzeug.security import generate_password_hash
import re
import MySQLdb
import os

app = Flask(__name__)

# Ensure debug is off in production so stack traces are not shown
app.config['DEBUG'] = False

USERNAME_REGEX = re.compile(r'^[A-Za-z0-9_]{3,20}$')
MIN_PASSWORD_LENGTH = 8

def get_db_connection():
    return MySQLdb.connect(
        host=os.environ.get('DB_HOST', 'localhost'),
        user=os.environ['DB_USER'],
        passwd=os.environ['DB_PASS'],
        db=os.environ['DB_NAME'],
        charset='utf8mb4',
        cursorclass=MySQLdb.cursors.DictCursor
    )

@app.route('/register', methods=['POST'])
def register():
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '')

    # 1) Validate inputs
    if not USERNAME_REGEX.match(username):
        return "Invalid username. Must be 3–20 chars; letters, numbers and underscores only.", 400

    if len(password) < MIN_PASSWORD_LENGTH:
        return f"Password must be at least {MIN_PASSWORD_LENGTH} characters long.", 400

    # 2) Hash the password
    hashed_password = generate_password_hash(password)

    # 3) Insert into DB
    try:
        db = get_db_connection()
        cursor = db.cursor()

        # Check for duplicate username
        cursor.execute("SELECT id FROM users WHERE username = %s", (username,))
        if cursor.fetchone():
            return "Username already taken.", 400

        # Insert new user
        cursor.execute(
            "INSERT INTO users (username, password) VALUES (%s, %s)",
            (username, hashed_password)
        )
        db.commit()

    except Exception as e:
        # Log full error server-side, but return a generic message
        current_app.logger.error("Error registering user %s: %s", username, str(e))
        return "An internal error occurred. Please try again later.", 500

    finally:
        try:
            cursor.close()
            db.close()
        except:
            pass

    # 4) On success, redirect to login
    return redirect('/login')