
from flask import Flask, request, redirect, abort
import re
import os
from argon2 import PasswordHasher
import MySQLdb
import MySQLdb.cursors

app = Flask(__name__)
# Ensure debug is off in production
app.config['DEBUG'] = False

# ...existing code...

@app.route('/register', methods=['POST'])
def register():
    # 1. Input validation & sanitization (whitelist approach)
    username = request.form.get('username', '').strip()
    if not re.fullmatch(r'[A-Za-z0-9_]{3,30}', username):
        abort(400, description='Invalid username format')
    password = request.form.get('password', '')
    if len(password) < 12:
        abort(400, description='Password must be ≥12 characters')

    # 2. Password hashing with Argon2 (defense-in-depth, per-user salt)
    ph = PasswordHasher(time_cost=2, memory_cost=102400, parallelism=8)
    try:
        password_hash = ph.hash(password)
    except Exception:
        abort(500, description='Server error during password hashing')

    # 3. Database interaction using least-privilege, env vars & parameterized queries
    db_params = {
        'host': os.getenv('DB_HOST', 'localhost'),
        'user': os.getenv('DB_USER'),
        'passwd': os.getenv('DB_PASSWORD'),
        'db': os.getenv('DB_NAME'),
        'charset': 'utf8mb4',
        'cursorclass': MySQLdb.cursors.DictCursor
    }
    try:
        conn = MySQLdb.connect(**db_params)
        cursor = conn.cursor()
        cursor.execute(
            "INSERT INTO users (username, password_hash) VALUES (%s, %s)",
            (username, password_hash)
        )
        conn.commit()
    except MySQLdb.IntegrityError:
        # Username uniqueness enforced at DB level
        abort(409, description='Username already exists')
    except Exception:
        abort(500, description='Database error')
    finally:
        cursor.close()
        conn.close()

    # 4. Post-registration redirect (no sensitive info in URL)
    return redirect('/login', code=302)

# ...existing code...