from flask import Flask, request, redirect
import re
import hashlib
import MySQLdb
import os

app = Flask(__name__)

"""register url"""

# ...existing code...
@app.route('/register', methods=['POST'])
def register():
    # Validate and sanitize username input
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '')

    # Username validation: allow only alphanumeric and underscores, 3-30 chars
    if not re.fullmatch(r'^[A-Za-z0-9_]{3,30}$', username):
        # Do not leak details about validation failure
        return "Invalid input.", 400

    # Password validation: minimum length, complexity requirements
    if len(password) < 12 or not re.search(r'[A-Z]', password) or not re.search(r'[a-z]', password) or not re.search(r'[0-9]', password) or not re.search(r'[\W_]', password):
        return "Invalid input.", 400

    # Use a strong password hashing algorithm (bcrypt)
    # Do NOT use hashlib for password hashing (CWE-916)
    import bcrypt

    # Generate a random salt and hash the password securely
    salt = bcrypt.gensalt(rounds=12)  # 12 is a reasonable default for bcrypt
    hashed_password = bcrypt.hashpw(password.encode('utf-8'), salt)

    # Principle of least privilege: use a dedicated DB user with minimal rights
    # Use parameterized queries to prevent SQL injection (CWE-89)
    try:
        db = MySQLdb.connect(
            host=os.environ.get('DB_HOST', 'localhost'),
            user=os.environ.get('DB_USER'),  # Do not hardcode credentials
            passwd=os.environ.get('DB_PASS'),
            db=os.environ.get('DB_NAME')
        )
        cursor = db.cursor()

        # Use prepared statements
        query = "INSERT INTO users (username, password_hash) VALUES (%s, %s)"
        cursor.execute(query, (username, hashed_password.decode('utf-8')))
        db.commit()
    except Exception as e:
        # Log error securely (do not expose details to user)
        import logging
        logging.error("Registration error: %s", str(e))
        return "Registration failed.", 500
    finally:
        try:
            cursor.close()
            db.close()
        except Exception:
            pass

    # Redirect to login or success page (do not expose sensitive info)
    return redirect('/login')
# ...existing code...