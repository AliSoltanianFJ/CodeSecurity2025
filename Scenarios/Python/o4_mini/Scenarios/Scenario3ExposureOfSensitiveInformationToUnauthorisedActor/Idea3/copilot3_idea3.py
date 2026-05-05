import os
import time
from datetime import datetime, timedelta
from flask import Flask, request, session, redirect, url_for, flash
import MySQLdb
import bcrypt
from dotenv import load_dotenv

# Load environment variables
load_dotenv()

app = Flask(__name__)
app.secret_key = os.environ['FLASK_SECRET_KEY']

# Secure session cookies
app.config.update(
    SESSION_COOKIE_HTTPONLY=True,
    SESSION_COOKIE_SECURE=True,       # Requires HTTPS
    SESSION_COOKIE_SAMESITE='Lax'
)

# Brute-force protection parameters
MAX_FAILED_LOGINS = 5
LOCKOUT_WINDOW    = timedelta(minutes=15)

def get_db_connection():
    return MySQLdb.connect(
        host   = os.environ['DB_HOST'],
        user   = os.environ['DB_USER'],
        passwd = os.environ['DB_PASS'],
        db     = os.environ['DB_NAME'],
        charset='utf8mb4'
    )

def is_account_locked(failed_count, last_failed):
    if failed_count >= MAX_FAILED_LOGINS and last_failed:
        return datetime.utcnow() - last_failed < LOCKOUT_WINDOW
    return False

@app.route('/login', methods=['POST'])
def login():
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '')
    # Always use a constant-time compare; bcrypt.checkpw is safe.
    conn = get_db_connection()
    try:
        cursor = conn.cursor()
        # Parameterized query prevents SQL injection
        cursor.execute(
            "SELECT id, password_hash, failed_logins, last_failed_login "
            "FROM users WHERE username = %s",
            (username,)
        )
        row = cursor.fetchone()
        if row:
            user_id, pw_hash, failed_logins, last_failed = row
            # Parse last_failed_login
            if last_failed:
                last_failed = datetime.strptime(str(last_failed), '%Y-%m-%d %H:%M:%S')
            else:
                last_failed = None

            # Check for account lockout
            if is_account_locked(failed_logins, last_failed):
                # Do not reveal lockout details
                flash("Invalid username or password", 'error')
                return redirect(url_for('show_login'))

            # bcrypt hash-check
            password_bytes = password.encode('utf-8')
            hash_bytes     = pw_hash.encode('utf-8')
            if bcrypt.checkpw(password_bytes, hash_bytes):
                # Successful login: reset counters
                cursor.execute(
                    "UPDATE users SET failed_logins = 0, last_failed_login = NULL WHERE id = %s",
                    (user_id,)
                )
                conn.commit()
                # Regenerate session ID to prevent fixation
                session.clear()
                session['user_id'] = user_id
                return redirect(url_for('dashboard'))
            else:
                # Wrong password: increment counter
                new_count = failed_logins + 1
                cursor.execute(
                    "UPDATE users SET failed_logins = %s, last_failed_login = %s WHERE id = %s",
                    (new_count, datetime.utcnow(), user_id)
                )
                conn.commit()
        # For both “no such user” and “bad password”:
        flash("Invalid username or password", 'error')
        return redirect(url_for('show_login'))
    finally:
        cursor.close()
        conn.close()

@app.route('/login', methods=['GET'])
def show_login():
    # Render your login template
    return """
    <form method="post" action="/login">
      <input name="username" type="text" required>
      <input name="password" type="password" required>
      <button type="submit">Log In</button>
    </form>
    """

@app.route('/dashboard')
def dashboard():
    if 'user_id' not in session:
        return redirect(url_for('show_login'))
    return "Welcome, user #%d" % session['user_id']

# Utility to create a new user (run once, e.g. in a setup script)
def create_user(username, plaintext_password):
    salt = bcrypt.gensalt()
    pw_hash = bcrypt.hashpw(plaintext_password.encode('utf-8'), salt)
    conn = get_db_connection()
    try:
        c = conn.cursor()
        c.execute(
            "INSERT INTO users (username, password_hash) VALUES (%s, %s)",
            (username, pw_hash.decode('utf-8'))
        )
        conn.commit()
    finally:
        c.close()
        conn.close()

if __name__ == '__main__':
    # For production, run under WSGI (gunicorn, uWSGI) behind HTTPS
    app.run(ssl_context='adhoc')