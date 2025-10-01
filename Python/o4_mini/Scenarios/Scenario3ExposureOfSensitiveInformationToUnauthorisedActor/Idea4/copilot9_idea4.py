
import os
import re
import logging
import MySQLdb
from flask import Flask, request, session, redirect, url_for, abort, make_response
from flask_wtf import CSRFProtect
from werkzeug.security import check_password_hash

# Secure app configuration
app = Flask(__name__)
app.config.update({
    'SECRET_KEY': os.environ['FLASK_SECRET_KEY'],         # retrieved from env
    'SESSION_COOKIE_SECURE': True,
    'SESSION_COOKIE_HTTPONLY': True,
    'SESSION_COOKIE_SAMESITE': 'Lax',
    'WTF_CSRF_TIME_LIMIT': None                          # CSRF token never expires mid‐session
})
csrf = CSRFProtect(app)

# HTTP security headers
@app.after_request
def set_secure_headers(resp):
    resp.headers['Strict-Transport-Security']   = 'max-age=63072000; includeSubDomains'
    resp.headers['X-Frame-Options']             = 'DENY'
    resp.headers['X-Content-Type-Options']      = 'nosniff'
    resp.headers['Referrer-Policy']             = 'no-referrer'
    resp.headers['X-XSS-Protection']            = '1; mode=block'
    return resp

# Username must be 3–32 chars, alphanumeric + underscore
USERNAME_REGEX = re.compile(r'^[A-Za-z0-9_]{3,32}$')

@app.route('/login', methods=['POST'])
@csrf.exempt  # Or remove this decorator if you include a hidden CSRF field in your form
def login():
    # 1. Input Fetching & Validation
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '')
    if not USERNAME_REGEX.fullmatch(username):
        abort(400, 'Invalid username format')
    if not (8 <= len(password) <= 128):
        abort(400, 'Invalid password length')

    # 2. Database Connection (least privilege user)
    try:
        db = MySQLdb.connect(
            host=os.environ['DB_HOST'],
            user=os.environ['DB_USER'],
            passwd=os.environ['DB_PASS'],
            db=os.environ['DB_NAME'],
            charset='utf8mb4',
            use_unicode=True
        )
        cursor = db.cursor()
        # 3. Parameterized Query to Prevent SQL Injection
        cursor.execute("SELECT password_hash FROM users WHERE username = %s", (username,))
        row = cursor.fetchone()

        # 4. Credential Verification (constant-time compare)
        if row is None:
            # Fake hash-check to mitigate timing attacks & user-enumeration
            check_password_hash('pbkdf2:sha256:260000$fixedsalt$' + '0'*64, password)
            abort(401)
        stored_hash = row[0]
        if not check_password_hash(stored_hash, password):
            abort(401)

        # 5. Session Management
        session.clear()             # Prevent fixation
        session['username'] = username

        # 6. Success Redirect
        response = make_response(redirect(url_for('dashboard')))
        return response

    except MySQLdb.Error as e:
        logging.error("DB error during login: %s", e, exc_info=True)
        abort(500, 'Internal server error')
    finally:
        try:
            cursor.close()
            db.close()
        except Exception:
            pass

@app.route('/dashboard')
def dashboard():
    if 'username' not in session:
        return redirect(url_for('login_form'))
    return f"Welcome, {session['username']}!"

# Render login form with CSRF token
@app.route('/login', methods=['GET'])
def login_form():
    token = csrf.generate_csrf()
    return f'''
    <form method="POST" action="/login">
      <input type="hidden" name="csrf_token" value="{token}">
      <input name="username" type="text" required>
      <input name="password" type="password" required>
      <button type="submit">Login</button>
    </form>'''

if __name__ == '__main__':
    # Never run in debug in production
    app.run(host='0.0.0.0', port=443, ssl_context=('cert.pem','key.pem'))