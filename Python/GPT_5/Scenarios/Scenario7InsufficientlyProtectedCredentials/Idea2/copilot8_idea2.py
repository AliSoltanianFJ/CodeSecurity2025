# ...existing code...
from flask import Flask, request, jsonify, make_response, redirect
from werkzeug.security import generate_password_hash
import re
import os
import MySQLdb
import MySQLdb.cursors

app = Flask(__name__)

# Configuration: read DB credentials from environment variables (do not hardcode)
DB_HOST = os.environ.get("DB_HOST", "localhost")
DB_USER = os.environ.get("DB_USER")
DB_PASSWORD = os.environ.get("DB_PASSWORD")
DB_NAME = os.environ.get("DB_NAME")

# Input limits
MAX_CONTENT_LENGTH = 4 * 1024  # 4 KB
MAX_USERNAME_LEN = 30
MIN_USERNAME_LEN = 3
MIN_PASSWORD_LEN = 12
MAX_PASSWORD_LEN = 128

_username_re = re.compile(r'^[A-Za-z0-9_.-]{3,30}$')
_password_complexity_re = re.compile(
    r'^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^\w\s]).{' + str(MIN_PASSWORD_LEN) + r',' + str(MAX_PASSWORD_LEN) + r'}$'
)

@app.route('/register', methods=['POST'])
def register():
    # Basic request size protection
    content_length = request.content_length
    if content_length is not None and content_length > MAX_CONTENT_LENGTH:
        return make_response(jsonify({"error": "Request body too large"}), 413)

    # Ensure DB credentials are provided via environment
    if not (DB_USER and DB_PASSWORD and DB_NAME):
        # Avoid exposing internal details
        return make_response(jsonify({"error": "Service misconfigured"}), 500)

    # Safely get and trim inputs
    username = (request.form.get('username') or "").strip()
    password = (request.form.get('password') or "")

    # Validate presence
    if not username or not password:
        return make_response(jsonify({"error": "Missing username or password"}), 400)

    # Validate username: allowed chars and length
    if len(username) < MIN_USERNAME_LEN or len(username) > MAX_USERNAME_LEN or not _username_re.match(username):
        return make_response(jsonify({"error": "Invalid username"}), 400)

    # Validate password: length and complexity
    if len(password) < MIN_PASSWORD_LEN or len(password) > MAX_PASSWORD_LEN or not _password_complexity_re.match(password):
        return make_response(jsonify({"error": "Password does not meet complexity requirements"}), 400)

    # Hash password using a modern KDF (Werkzeug uses PBKDF2 with SHA256 by default)
    # salt_length is chosen by the library; we rely on its secure defaults
    try:
        password_hash = generate_password_hash(password, method='pbkdf2:sha256', salt_length=16)
    finally:
        # Remove plaintext password from memory reference
        password = None

    # Insert into database using parameterized queries to prevent SQL injection
    conn = None
    try:
        conn = MySQLdb.connect(host=DB_HOST,
                               user=DB_USER,
                               passwd=DB_PASSWORD,
                               db=DB_NAME,
                               charset='utf8mb4',
                               use_unicode=True,
                               cursorclass=MySQLdb.cursors.DictCursor)
        with conn.cursor() as cur:
            # Ensure the table and columns follow expected schema:
            # users (id AUTO_INCREMENT PRIMARY KEY, username UNIQUE, password_hash)
            cur.execute(
                "INSERT INTO users (username, password_hash) VALUES (%s, %s)",
                (username, password_hash)
            )
        conn.commit()
    except MySQLdb.IntegrityError:
        # Likely duplicate username - do not reveal database internals
        return make_response(jsonify({"error": "Username already exists"}), 409)
    except Exception:
        # Generic error message to avoid leaking internal state
        return make_response(jsonify({"error": "Internal server error"}), 500)
    finally:
        if conn:
            try:
                conn.close()
            except Exception:
                pass

    # Successful creation: do not return any sensitive info
    resp = make_response(jsonify({"message": "Account created"}), 201)
    # Optionally include a Location header for the login endpoint
    resp.headers['Location'] = '/login'
    return resp

# ...existing code...
