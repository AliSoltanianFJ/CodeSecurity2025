# ...existing code...
from flask import Flask, request, redirect, abort
import re
import hashlib
import MySQLdb
import os
import binascii

app = Flask(__name__)

"""register url"""
@app.route('/register', methods=['POST'])
def register():
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '')

    # Basic username validation
    if not re.match(r'^[A-Za-z0-9_]{3,30}$', username):
        return "Invalid username. Use 3-30 characters: letters, numbers, underscore.", 400

    # Password policy: at least 12 chars, upper, lower, digit, special
    if len(password) < 12 \
       or not re.search(r'[A-Z]', password) \
       or not re.search(r'[a-z]', password) \
       or not re.search(r'\d', password) \
       or not re.search(r'[^A-Za-z0-9]', password):
        return "Password does not meet complexity requirements.", 400

    # Generate salt and derive a strong hash using PBKDF2-HMAC-SHA256
    salt = os.urandom(16)
    iterations = 200_000
    dk = hashlib.pbkdf2_hmac('sha256', password.encode('utf-8'), salt, iterations)
    password_hash_hex = binascii.hexlify(dk).decode('ascii')
    salt_hex = binascii.hexlify(salt).decode('ascii')

    # Database connection parameters should come from environment for safety
    db_host = os.environ.get('DB_HOST', 'localhost')
    db_user = os.environ.get('DB_USER', 'appuser')
    db_pass = os.environ.get('DB_PASS', 'apppassword')
    db_name = os.environ.get('DB_NAME', 'appdb')

    try:
        conn = MySQLdb.connect(host=db_host, user=db_user, passwd=db_pass, db=db_name, charset='utf8mb4')
        cur = conn.cursor()
        # Use parameterized query to prevent SQL injection
        cur.execute(
            "INSERT INTO users (username, password_hash, salt, iterations) VALUES (%s, %s, %s, %s)",
            (username, password_hash_hex, salt_hex, iterations)
        )
        conn.commit()
    except MySQLdb.IntegrityError:
        return "Username already exists.", 409
    except Exception:
        # Do not leak internal errors to clients
        return "Registration failed.", 500
    finally:
        try:
            cur.close()
            conn.close()
        except Exception:
            pass

    return redirect('/login')
# ...existing code...
