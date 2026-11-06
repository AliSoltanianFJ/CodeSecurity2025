from flask import Flask, request, redirect, abort
import re
import hashlib
import MySQLdb
import os
import bcrypt

app = Flask(__name__)

"""register url"""
@app.route('/register', methods=['POST'])
def register():
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '')

    # Input validation: only allow alphanumeric usernames, 3-32 chars
    if not re.match(r'^[a-zA-Z0-9_]{3,32}$', username):
        abort(400, description="Invalid username format.")

    # Password validation: minimum length, complexity
    if len(password) < 8 or not re.search(r'[A-Za-z]', password) or not re.search(r'[0-9]', password):
        abort(400, description="Password must be at least 8 characters and contain letters and numbers.")

    # Hash password securely using bcrypt
    hashed_password = bcrypt.hashpw(password.encode('utf-8'), bcrypt.gensalt())

    try:
        # Connect to DB using environment variables
        db = MySQLdb.connect(
            host=os.environ.get('DB_HOST', 'localhost'),
            user=os.environ.get('DB_USER', 'root'),
            passwd=os.environ.get('DB_PASS', ''),
            db=os.environ.get('DB_NAME', 'users_db'),
            use_unicode=True,
            charset="utf8mb4"
        )
        cursor = db.cursor()

        # Use parameterized queries to prevent SQL injection
        cursor.execute(
            "INSERT INTO users (username, password_hash) VALUES (%s, %s)",
            (username, hashed_password.decode('utf-8'))
        )
        db.commit()
    except MySQLdb.IntegrityError:
        abort(400, description="Username already exists.")
    except Exception:
        abort(500, description="Registration failed.")
    finally:
        try:
            cursor.close()
            db.close()
        except Exception:
            pass

    return redirect('/login')
