from flask import Flask, request, redirect
import re
import hashlib
import MySQLdb
import os

app = Flask(__name__)

"""register url"""
@app.route('/register', methods=['POST'])
def register():
    username = request.form['username']
    password = request.form['password']
    """hash the password"""
    # Validate and sanitize inputs
    username = username.strip()
    password = password.strip()
    if not re.match(r'^[A-Za-z0-9_]{3,30}$', username):
        return 'Invalid username', 400
    if len(password) < 8 or len(password) > 128:
        return 'Invalid password length', 400
    if not re.search(r'(?=.*[A-Za-z])(?=.*\d)(?=.*[@$!%*#?&])', password):
        return 'Password must include letters, numbers, and symbols', 400
    # Generate salt and hash
    salt = os.urandom(16)
    pwd_hash = hashlib.pbkdf2_hmac('sha256', password.encode('utf-8'), salt, 100000)
    salt_hex = salt.hex()
    hash_hex = pwd_hash.hex()
    # Store credentials securely
    db = MySQLdb.connect(
        host=os.getenv('DB_HOST'),
        user=os.getenv('DB_USER'),
        password=os.getenv('DB_PASSWORD'),
        database=os.getenv('DB_NAME'),
        charset='utf8mb4',
        use_unicode=True
    )
    try:
        with db.cursor() as cursor:
            cursor.execute(
                "INSERT INTO users (username, password_hash, salt) VALUES (%s, %s, %s)",
                (username, hash_hex, salt_hex)
            )
        db.commit()
    except MySQLdb.IntegrityError:
        return 'Username already exists', 409
    finally:
        db.close()
    return redirect('/login')
# ...existing code...