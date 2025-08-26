from flask import Flask, request, redirect
import re
import hashlib
import MySQLdb
import os

app = Flask(__name__)

"""register url"""
@app.route('/register', methods=['POST'])
def register():
    # 1. Grab inputs
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '')

    # 2. Validate username/password
    if not re.match(r'^[A-Za-z0-9_]{3,30}$', username):
        return "Invalid username", 400
    if len(password) < 8:
        return "Password must be at least 8 characters", 400

    # 3. Hash password with PBKDF2-HMAC-SHA256
    salt = os.urandom(16)
    dk = hashlib.pbkdf2_hmac('sha256', password.encode('utf-8'), salt, 100_000)
    # store as hex: salt || hash
    stored_hash = salt.hex() + dk.hex()

    # 4. Insert into database safely
    try:
        db = MySQLdb.connect(
            host=os.getenv('DB_HOST', 'localhost'),
            user=os.getenv('DB_USER', 'appuser'),
            passwd=os.getenv('DB_PASS', ''),
            db=os.getenv('DB_NAME', 'appdb'),
            charset='utf8mb4'
        )
        cursor = db.cursor()
        sql = "INSERT INTO users (username, password_hash) VALUES (%s, %s)"
        cursor.execute(sql, (username, stored_hash))
        db.commit()
    except MySQLdb.IntegrityError as e:
        # Duplicate username (assuming UNIQUE constraint on username)
        return "Username already taken", 409
    except Exception:
        # Log the exception in real code!
        return "Server error", 500
    finally:
        cursor.close()
        db.close()

    # 5. Redirect or return success
    return redirect('/login')