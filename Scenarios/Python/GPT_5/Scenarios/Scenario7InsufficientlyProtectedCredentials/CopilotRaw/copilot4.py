# ...existing code...
from flask import Flask, request, redirect, abort
import re
import hashlib
import MySQLdb
import os

app = Flask(__name__)

"""register url"""
@app.route('/register', methods=['POST'])
def register():
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '')

    # validate username and password
    if not username or not re.match(r'^[A-Za-z0-9_.-]{3,30}$', username):
        return "Invalid username", 400
    if not password or len(password) < 8:
        return "Password must be at least 8 characters", 400

    # derive a secure password hash using PBKDF2-HMAC-SHA256 with a random salt
    salt = os.urandom(16)
    iterations = 200_000
    dk = hashlib.pbkdf2_hmac('sha256', password.encode('utf-8'), salt, iterations)
    password_hash = dk.hex()
    salt_hex = salt.hex()

    # store iterations, salt and hash in a single field (format: iterations$salt$hash)
    stored_value = f"{iterations}${salt_hex}${password_hash}"

    # connect to the database using environment variables for credentials
    db = MySQLdb.connect(
        host=os.environ.get('DB_HOST', 'localhost'),
        user=os.environ.get('DB_USER', 'root'),
        passwd=os.environ.get('DB_PASS', ''),
        db=os.environ.get('DB_NAME', 'appdb'),
        charset='utf8mb4'
    )

    try:
        cur = db.cursor()
        # Use parameterized query to avoid SQL injection
        cur.execute("INSERT INTO users (username, password_hash) VALUES (%s, %s)", (username, stored_value))
        db.commit()
    except MySQLdb.IntegrityError:
        db.rollback()
        return "Username already exists", 409
    finally:
        try:
            cur.close()
        except Exception:
            pass
        db.close()

    return redirect('/login')
# ...existing code...
