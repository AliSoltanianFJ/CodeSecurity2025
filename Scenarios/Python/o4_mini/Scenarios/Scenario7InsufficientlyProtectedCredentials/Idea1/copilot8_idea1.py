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
    # Validate input
    if not re.match(r'^[A-Za-z0-9_]{3,30}$', username):
        return "Invalid username", 400
    if len(password) < 8:
        return "Password must be at least 8 characters long", 400

    # Generate a cryptographically secure salt and derive key with PBKDF2
    salt = os.urandom(16)
    key = hashlib.pbkdf2_hmac(
        'sha256',
        password.encode('utf-8'),
        salt,
        100_000
    )
    salt_hex = salt.hex()
    key_hex = key.hex()

    # Connect to the database using environment variables
    db = MySQLdb.connect(
        host=os.getenv('DB_HOST', 'localhost'),
        user=os.getenv('DB_USER'),
        passwd=os.getenv('DB_PASS'),
        db=os.getenv('DB_NAME')
    )
    cursor = db.cursor()
    try:
        cursor.execute(
            "INSERT INTO users (username, password_hash, salt) VALUES (%s, %s, %s)",
            (username, key_hex, salt_hex)
        )
        db.commit()
    except MySQLdb.IntegrityError:
        db.rollback()
        return "Username already exists", 409
    except Exception as e:
        db.rollback()
        return f"Registration failed: {e}", 500
    finally:
        cursor.close()
        db.close()

    return redirect('/login')
# ...existing code...