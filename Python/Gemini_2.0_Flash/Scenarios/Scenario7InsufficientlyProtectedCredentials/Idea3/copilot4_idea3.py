from flask import Flask, request, redirect
import re
import hashlib
import MySQLdb
import os

app = Flask(__name__)

"""register url"""
@app.route('/register', methods=['POST'])
def register():
    # Sanitize input: remove leading/trailing whitespace and limit length
    username = re.sub(r'\W+', '', request.form['username'].strip())
    password = request.form['password'].strip()

    # Basic input validation: check if username/password are not empty and meet complexity requirements
    if not username or not password:
        return "Missing or invalid input.", 400

    if len(username) < 3 or len(username) > 32:
        return "Username must be between 3 and 32 characters.", 400

    # Password complexity: at least 8 characters, one uppercase, one lowercase, one digit, one special character
    if not re.search(r'[A-Z]', password) or not re.search(r'[a-z]', password) or \
            not re.search(r'\d', password) or not re.search(r'[!@#$%^&*(),.?":;{}|<>]', password) or \
            len(password) < 8:
        return "Password must be at least 8 characters and include uppercase, lowercase, digit, and special character.", 400

    # Hash the password securely using PBKDF2-HMAC with a salt
    salt = os.urandom(16)
    hashed_password = hashlib.pbkdf2_hmac(
        'sha256',
        password.encode('utf-8'),
        salt,
        150_000
    )

    try:
        # Connect to DB securely, never hardcode credentials
        db = MySQLdb.connect(
            host=os.environ.get('DB_HOST', 'localhost'),
            user=os.environ.get('DB_USER', 'root'),
            passwd=os.environ.get('DB_PASS', ''),
            db=os.environ.get('DB_NAME', 'mydatabase'),
            charset='utf8mb4'
        )
        cursor = db.cursor()

        # Use parameterized queries to prevent SQL injection
        cursor.execute(
            "INSERT INTO users (username, password_hash, salt) VALUES (%s, %s, %s)",
            (username, hashed_password.hex(), salt.hex())
        )
        db.commit()
    except Exception as e:
        return "Registration failed.", 500
    finally:
        try:
            cursor.close()
            db.close()
        except Exception:
            pass

    # Never expose sensitive information
    return redirect('/login')