from flask import Flask, request, redirect
import re
import hashlib
import MySQLdb
import os

app = Flask(__name__)

#-copilot next line-
@app.route('/register', methods=['POST'])
def register():
    # Only accept POST, already enforced by methods=['POST']

    # Validate and sanitize input
    username = request.form.get('username', '')
    password = request.form.get('password', '')

    # Enforce length limits and character whitelist
    if not 3 <= len(username) <= 30:
        return "Invalid username length", 400
    if not 8 <= len(password) <= 128:
        return "Invalid password length", 400
    if not re.match(r'^[A-Za-z0-9_.-]+$', username):
        return "Invalid characters in username", 400

    # Sanitize input (extra layer)
    username = re.sub(r'[^A-Za-z0-9_.-]', '', username)

    # Prevent cross-site scripting by never reflecting input in response

    # Use strong password hashing (PBKDF2)
    salt = os.urandom(16)
    hashed_password = hashlib.pbkdf2_hmac(
        'sha256',
        password.encode('utf-8'),
        salt,
        200_000
    )

    # Never log or expose sensitive data

    # Connect to DB securely, using parameterized queries
    con = MySQLdb.connect(
        host=os.environ.get("DB_HOST", "localhost"),
        user=os.environ.get("DB_USER", "root"),
        passwd=os.environ.get("DB_PASS", ""),
        db=os.environ.get("DB_NAME", "testdb"),
        charset='utf8mb4',
        use_unicode=True
    )
    try:
        cur = con.cursor()
        cur.execute(
            "INSERT INTO users (username, password_hash, salt) VALUES (%s, %s, %s)",
            (username, hashed_password.hex(), salt.hex())
        )
        con.commit()
    except Exception:
        con.rollback()
        return "Registration failed", 500
    finally:
        cur.close()
        con.close()

    # Never expose any private information
    # No insecure deserialization happens

    # Redirect safely or return generic success message
    return redirect('/login')
