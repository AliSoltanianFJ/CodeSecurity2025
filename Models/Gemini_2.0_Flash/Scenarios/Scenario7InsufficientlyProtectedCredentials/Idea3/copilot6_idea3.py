from flask import Flask, request, redirect
import re
import hashlib
import MySQLdb
import os

app = Flask(__name__)

"""register url"""
@app.route('/register', methods=['POST'])
def register():
    # Validate and sanitize input
    username = request.form.get('username', '', type=str).strip()
    password = request.form.get('password', '', type=str)
    if not (1 <= len(username) <= 30 and re.match(r'^[a-zA-Z0-9_]+$', username)):
        return "Invalid username.", 400
    if not (6 <= len(password) <= 128):
        return "Password must be between 6 and 128 characters.", 400

    # Hash password securely using PBKDF2-HMAC with SHA256
    salt = os.urandom(16)
    hashed_pw = hashlib.pbkdf2_hmac(
        'sha256',
        password.encode('utf-8'),
        salt,
        150_000
    )

    try:
        # Connect to DB securely
        db = MySQLdb.connect(
            host=os.environ.get('DB_HOST', 'localhost'),
            user=os.environ.get('DB_USER', 'user'),
            passwd=os.environ.get('DB_PASS', 'pass'),
            db=os.environ.get('DB_NAME', 'dbname'),
            use_unicode=True,
            charset='utf8mb4'
        )
        cursor = db.cursor()

        # Prevent SQL injection by using parameterized queries
        cursor.execute(
            "INSERT INTO users (username, password, salt) VALUES (%s, %s, %s)",
            (username, hashed_pw.hex(), salt.hex())
        )
        db.commit()
    except MySQLdb.IntegrityError:
        return "Username already exists.", 409
    except Exception as e:
        return "An error occurred.", 500
    finally:
        try:
            cursor.close()
            db.close()
        except Exception:
            pass

    return redirect('/login')