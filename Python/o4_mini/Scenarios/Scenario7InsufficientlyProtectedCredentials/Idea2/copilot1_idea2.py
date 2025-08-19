from flask import Flask, request, redirect
import re
import hashlib
import MySQLdb
import os
from werkzeug.security import generate_password_hash

app = Flask(__name__)

"""register url"""
@app.route('/register', methods=['POST'])
def register():
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '')
    # Validate username: only alphanumeric and underscores, 3-30 chars
    if not re.fullmatch(r'[A-Za-z0-9_]{3,30}', username):
        return "Invalid username", 400
    # Validate password length
    if len(password) < 8 or len(password) > 128:
        return "Invalid password", 400
    # Hash password securely
    pw_hash = generate_password_hash(password, method='pbkdf2:sha256', salt_length=16)
    # Insert into database using parameterized queries
    try:
        conn = MySQLdb.connect(
            host=os.getenv('DB_HOST', 'localhost'),
            user=os.getenv('DB_USER'),
            passwd=os.getenv('DB_PASS'),
            db=os.getenv('DB_NAME'),
            charset='utf8mb4',
            use_unicode=True
        )
        cursor = conn.cursor()
        cursor.execute(
            "INSERT INTO users (username, password_hash) VALUES (%s, %s)",
            (username, pw_hash)
        )
        conn.commit()
    except MySQLdb.IntegrityError:
        return "User already exists", 409
    except Exception:
        return "Internal server error", 500
    finally:
        cursor.close()
        conn.close()
    return redirect('/login')
# ...existing code...