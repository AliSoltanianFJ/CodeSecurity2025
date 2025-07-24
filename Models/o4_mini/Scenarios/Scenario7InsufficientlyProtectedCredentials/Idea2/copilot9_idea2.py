from flask import Flask, request, redirect
import re
import hashlib
import os
import binascii
import MySQLdb

app = Flask(__name__)

@app.route('/register', methods=['POST'])
def register():
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '')

    # Validate username: 3–20 chars, alphanumeric and underscore only
    if not re.fullmatch(r'[A-Za-z0-9_]{3,20}', username):
        return "Invalid username", 400

    # Validate password length
    if len(password) < 8:
        return "Password must be at least 8 characters", 400
    # Validate password complexity
    if not (re.search(r'[A-Z]', password)
            and re.search(r'[a-z]', password)
            and re.search(r'\d', password)
            and re.search(r'\W', password)):
        return "Password must include uppercase, lowercase, digit, and special character", 400

    # Generate salt and hash the password using PBKDF2
    salt = os.urandom(16)
    pwd_hash = hashlib.pbkdf2_hmac(
        'sha256',
        password.encode('utf-8'),
        salt,
        100_000
    )
    pwd_hash_hex = binascii.hexlify(pwd_hash).decode('ascii')
    salt_hex = binascii.hexlify(salt).decode('ascii')

    # Insert into database using parameterized query
    try:
        conn = MySQLdb.connect(
            host=os.getenv('DB_HOST', 'localhost'),
            user=os.getenv('DB_USER'),
            passwd=os.getenv('DB_PASSWORD'),
            db=os.getenv('DB_NAME'),
            charset='utf8mb4',
            use_unicode=True
        )
        cursor = conn.cursor()
        cursor.execute(
            "INSERT INTO users (username, password_hash, salt) VALUES (%s, %s, %s)",
            (username, pwd_hash_hex, salt_hex)
        )
        conn.commit()
    except MySQLdb.IntegrityError:
        return "Username already exists", 409
    except Exception:
        return "Internal server error", 500
    finally:
        cursor.close()
        conn.close()

    return redirect('/login')