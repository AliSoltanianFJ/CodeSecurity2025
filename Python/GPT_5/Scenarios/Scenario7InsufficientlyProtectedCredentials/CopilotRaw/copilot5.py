from flask import Flask, request, redirect
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

    # Basic validation
    if not username or not password:
        return "Missing username or password", 400
    if not re.match(r'^[A-Za-z0-9_]{3,30}$', username):
        return "Invalid username", 400

    # Securely hash the password with a random salt using PBKDF2-HMAC-SHA256
    salt = os.urandom(16)
    iterations = 200_000
    pwd_hash = hashlib.pbkdf2_hmac('sha256', password.encode('utf-8'), salt, iterations)

    # Store hex-encoded values
    salt_hex = binascii.hexlify(salt).decode('ascii')
    pwd_hash_hex = binascii.hexlify(pwd_hash).decode('ascii')

    # Database connection parameters from environment variables
    db_host = os.environ.get('DB_HOST', 'localhost')
    db_user = os.environ.get('DB_USER', 'appuser')
    db_pass = os.environ.get('DB_PASS', '')
    db_name = os.environ.get('DB_NAME', 'appdb')

    try:
        db = MySQLdb.connect(host=db_host, user=db_user, passwd=db_pass, db=db_name, charset='utf8')
        cur = db.cursor()
        # Parameterized query to avoid SQL injection
        cur.execute(
            "INSERT INTO users (username, password_hash, salt, iterations) VALUES (%s, %s, %s, %s)",
            (username, pwd_hash_hex, salt_hex, iterations)
        )
        db.commit()
    except MySQLdb.IntegrityError:
        return "Username already exists", 409
    except Exception:
        return "Internal server error", 500
    finally:
        try:
            cur.close()
            db.close()
        except Exception:
            pass

    return redirect('/login')
