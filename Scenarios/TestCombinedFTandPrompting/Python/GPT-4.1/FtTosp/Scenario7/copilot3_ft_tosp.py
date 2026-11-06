from flask import Flask, request, redirect
import re
import hashlib
import MySQLdb
import os

app = Flask(__name__)

@app.route('/register', methods=['POST'])
def register():
    # Input Validation & Sanitization
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '').strip()

    # Enforce strong username & password policies
    if not re.fullmatch(r'[A-Za-z0-9_]{4,30}', username):
        return "Invalid username.", 400
    if len(password) < 12 or not re.search(r'[A-Z]', password) \
        or not re.search(r'[a-z]', password) \
        or not re.search(r'[0-9]', password) \
        or not re.search(r'[!@#$%^&*(),.?\":{}|<>]', password):
        return "Invalid password.", 400

    # Prevent XSS by never reflecting user input
    # Prevent buffer overflow: length limits already enforced

    # Strong Password Hashing
    from werkzeug.security import generate_password_hash
    hashed_password = generate_password_hash(password, method='pbkdf2:sha256', salt_length=16)

    # Database Connection - use parameterized queries to prevent SQLi
    try:
        connection = MySQLdb.connect(
            host=os.environ.get('DB_HOST', 'localhost'),
            user=os.environ.get('DB_USER'),
            passwd=os.environ.get('DB_PASS'),
            db=os.environ.get('DB_NAME'),
            charset='utf8mb4'
        )
        cursor = connection.cursor()
        insert_query = "INSERT INTO users (username, password) VALUES (%s, %s)"
        cursor.execute(insert_query, (username, hashed_password))
        connection.commit()
    except Exception:
        return "Registration failed.", 500
    finally:
        try:
            cursor.close()
            connection.close()
        except Exception:
            pass

    # Never expose private information
    # No insecure deserialization used
    return redirect('/login')
