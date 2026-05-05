
import os
import re
import logging
from flask import Flask, request, redirect, url_for, make_response, abort
import MySQLdb

# --- Configuration & Secure Defaults ---
app = Flask(__name__)
app.config.update(
    SECRET_KEY=os.getenv('FLASK_SECRET_KEY', os.urandom(32)),
    DB_HOST=os.getenv('DB_HOST', 'localhost'),
    DB_USER=os.getenv('DB_USER', 'readonly_user'),
    DB_PASS=os.getenv('DB_PASS'),
    DB_NAME=os.getenv('DB_NAME', 'subscribers_db'),
    SESSION_COOKIE_SECURE=True,
    SESSION_COOKIE_HTTPONLY=True,
    SESSION_COOKIE_SAMESITE='Lax'
)
app.debug = False
logging.basicConfig(level=logging.INFO)

# Strict email whitelist regex
EMAIL_REGEX = re.compile(r'^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$')

def get_db_connection():
    """Create a new MySQLdb connection using least‐privilege credentials."""
    return MySQLdb.connect(
        host=app.config['DB_HOST'],
        user=app.config['DB_USER'],
        passwd=app.config['DB_PASS'],
        db=app.config['DB_NAME'],
        charset='utf8mb4',
        use_unicode=True
    )

@app.after_request
def set_security_headers(response):
    response.headers['Strict-Transport-Security'] = 'max-age=63072000; includeSubDomains'
    response.headers['X-Content-Type-Options'] = 'nosniff'
    response.headers['X-Frame-Options'] = 'DENY'
    response.headers['Referrer-Policy'] = 'no-referrer'
    return response

@app.route('/unsubscribe', methods=['GET'])
def unsubscribe():
    # 1. Input Validation & Canonicalization
    email = request.args.get('email', '').strip()
    if not email:
        logging.warning('Unsubscribe attempt without email')
        abort(400, description='Missing email parameter')
    if not EMAIL_REGEX.fullmatch(email):
        logging.warning('Invalid email format: %s', email)
        abort(400, description='Invalid email format')

    # 2. Database Operation with Parameterized Query
    try:
        conn = get_db_connection()
        with conn.cursor() as cursor:
            cursor.execute(
                "DELETE FROM subscribers WHERE email = %s",
                (email,)
            )
        conn.commit()
    except MySQLdb.Error as e:
        logging.error('Database error during unsubscribe: %s', e)
        abort(500, description='Internal server error')
    finally:
        conn.close()

    # 3. Safe Redirect
    return redirect(url_for('goodbye'))

@app.route('/goodbye', methods=['GET'])
def goodbye():
    # Simple safe HTML response without reflection
    return make_response(
        "<h1>You have been unsubscribed.</h1>",
        200,
        {'Content-Type': 'text/html; charset=utf-8'}
    )

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000, ssl_context='adhoc')