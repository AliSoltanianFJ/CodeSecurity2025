
from flask import Flask, request, redirect, abort
from werkzeug.security import generate_password_hash
import re, os, MySQLdb
from dotenv import load_dotenv

# Load and lock down environment‐based secrets
load_dotenv()
DB_CONFIG = {
    'host': os.getenv('DB_HOST'),
    'user': os.getenv('DB_USER'),
    'passwd': os.getenv('DB_PASSWORD'),
    'db': os.getenv('DB_NAME'),
    'charset': 'utf8mb4',
    'cursorclass': MySQLdb.cursors.DictCursor
}

app = Flask(__name__)
# Disable debug in production
app.config.update(DEBUG=False,
                  SESSION_COOKIE_HTTPONLY=True,
                  SESSION_COOKIE_SECURE=True,
                  SESSION_COOKIE_SAMESITE='Lax')

# Defense-in-depth headers
@app.after_request
def set_security_headers(resp):
    resp.headers['Content-Security-Policy'] = "default-src 'self'"
    resp.headers['X-Frame-Options'] = 'DENY'
    resp.headers['X-Content-Type-Options'] = 'nosniff'
    resp.headers['Referrer-Policy'] = 'no-referrer'
    resp.headers['Strict-Transport-Security'] = 'max-age=31536000; includeSubDomains; preload'
    return resp

def get_db_connection():
    return MySQLdb.connect(**DB_CONFIG)

@app.route('/register', methods=['POST'])
def register():
    # 1. Retrieve and canonicalize inputs
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '')

    # 2. Input validation (whitelist: alphanumeric + underscore, 3–30 chars)
    if not re.fullmatch(r'[A-Za-z0-9_]{3,30}', username):
        abort(400, 'Invalid username format')
    if len(password) < 12:
        abort(400, 'Password too short')

    # 3. Strong password hashing (PBKDF2-SHA256 with salt)
    password_hash = generate_password_hash(password, method='pbkdf2:sha256', salt_length=16)

    # 4. Store with parameterized query, DB user has minimal privileges
    conn = get_db_connection()
    try:
        with conn.cursor() as cursor:
            sql = "INSERT INTO users (username, password_hash) VALUES (%s, %s)"
            cursor.execute(sql, (username, password_hash))
        conn.commit()
    except MySQLdb.IntegrityError:
        conn.rollback()
        abort(409, 'Username already exists')
    finally:
        conn.close()

    # 5. Redirect with 303 See Other to prevent form resubmission
    return redirect('/login', code=303)