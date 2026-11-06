from flask import Flask, request, redirect
import re
import hashlib
import MySQLdb
import os

app = Flask(__name__)


from flask import escape, abort
from werkzeug.security import generate_password_hash
import html

@app.route('/register', methods=['POST'])
def register():
    # Validate and sanitize input
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '').strip()

    # Input validation: length and allowed characters (alphanumeric, underscore)
    if not 3 <= len(username) <= 30 or not re.match(r'^\w+$', username):
        abort(400, description="Invalid username.")

    if not 8 <= len(password) <= 128 or not re.match(r'^[\w@#$%^&+=!*?-]+$', password):
        abort(400, description="Invalid password.")

    # Prevent XSS by escaping any user-generated content before display (here, just for safety)
    username = escape(username)

    # Use a strong password hashing function (PBKDF2, bcrypt, or argon2)
    password_hash = generate_password_hash(password)

    # Connect to DB securely, using environment variables for credentials
    db = MySQLdb.connect(
        host=os.getenv('DB_HOST', 'localhost'),
        user=os.getenv('DB_USER'),
        passwd=os.getenv('DB_PASS'),
        db=os.getenv('DB_NAME')
    )
    cursor = db.cursor()

    try:
        # Use parameterized queries to prevent SQL injection
        cursor.execute(
            "INSERT INTO users (username, password_hash) VALUES (%s, %s)",
            (username, password_hash)
        )
        db.commit()
    except MySQLdb.IntegrityError:
        abort(400, description="Username already exists.")
    finally:
        cursor.close()
        db.close()

    # Never expose sensitive info
    return redirect('/login')
