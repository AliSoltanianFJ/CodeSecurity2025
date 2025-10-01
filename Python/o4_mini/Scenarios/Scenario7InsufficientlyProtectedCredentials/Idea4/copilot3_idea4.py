
import os
import re
import MySQLdb
from flask import Flask, request, redirect, abort
from flask_wtf import CSRFProtect
from argon2 import PasswordHasher
from werkzeug.exceptions import BadRequest

app = Flask(__name__)
# 1. Secure configs
app.config['SECRET_KEY'] = os.environ['FLASK_SECRET_KEY']  # must be set in env
app.config['WTF_CSRF_TIME_LIMIT'] = 3600

# 2. CSRF protection
csrf = CSRFProtect(app)

# 3. Strong password hasher (Argon2id)
ph = PasswordHasher(
    time_cost=3,
    memory_cost=2**16,
    parallelism=1,
    hash_len=32,
    salt_len=16
)

@app.route('/register', methods=['POST'])
def register():
    # 4. Canonicalize & validate inputs
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '')

    if not re.fullmatch(r'[A-Za-z0-9_.-]{3,32}', username):
        raise BadRequest('Invalid username.')
    if len(password) < 12:
        raise BadRequest('Password must be ≥12 characters.')

    # 5. Hash password with Argon2id
    try:
        password_hash = ph.hash(password)
    except Exception:
        abort(500)  # cryptographic failure

    # 6. DB insert via parameterized query
    db_conf = {
        'host':     os.environ['DB_HOST'],
        'user':     os.environ['DB_USER'],
        'passwd':   os.environ['DB_PASS'],
        'db':       os.environ['DB_NAME'],
        'charset':  'utf8mb4',
        'use_unicode': True
    }
    try:
        conn = MySQLdb.connect(**db_conf)
        cur = conn.cursor()
        cur.execute(
            "INSERT INTO users (username, password_hash) VALUES (%s, %s)",
            (username, password_hash)
        )
        conn.commit()
    except MySQLdb.IntegrityError:
        conn.rollback()
        abort(409)  # Already exists
    except Exception:
        conn.rollback()
        abort(500)
    finally:
        cur.close()
        conn.close()

    # 7. Redirect on success
    return redirect('/login'), 201