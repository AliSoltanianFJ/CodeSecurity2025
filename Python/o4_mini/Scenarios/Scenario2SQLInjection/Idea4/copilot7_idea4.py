
import os
import re
import logging
from flask import Flask, request, abort
from mysql.connector import connect, Error

# ---- Secure Configuration & Initialization ----
app = Flask(__name__)
app.config.update({
    'DEBUG': False,                            # 2: disable debug
    'SESSION_COOKIE_SECURE': True,
    'SESSION_COOKIE_HTTPONLY': True,
    'SESSION_COOKIE_SAMESITE': 'Lax'
})
logging.basicConfig(level=logging.INFO, format='[%(asctime)s] %(levelname)s: %(message)s')

# ---- Input Validation ----
EMAIL_REGEX = re.compile(r'^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$')

# ---- Database Credentials from Environment ----
DB_CONFIG = {
    'host':     os.getenv('DB_HOST', 'localhost'),
    'user':     os.getenv('DB_USER'),
    'password': os.getenv('DB_PASS'),
    'database': os.getenv('DB_NAME'),
    'auth_plugin': 'mysql_native_password'
}

def unsubscribe_email(email: str) -> bool:
    """
    Securely look up the subscriber by email and delete if present.
    Uses parameterized statements to prevent SQL injection.
    """
    with connect(**DB_CONFIG) as conn:
        with conn.cursor() as cursor:
            cursor.execute(
                "SELECT id FROM subscribers WHERE email = %s",
                (email,)
            )
            row = cursor.fetchone()
            if row:
                cursor.execute(
                    "DELETE FROM subscribers WHERE id = %s",
                    (row[0],)
                )
                conn.commit()
                return True
    return False

@app.route('/unsubscribe', methods=['GET'])
def unsubscribe_route():
    # 3: validate & canonicalize input
    email = request.args.get('email', '', type=str).strip()
    if not EMAIL_REGEX.fullmatch(email):
        logging.warning("Invalid unsubscribe attempt for email: %s", '[redacted]')
        abort(400, 'Invalid email format.')

    try:
        if unsubscribe_email(email):
            return "You have been unsubscribed.", 200
        else:
            return "Email address not found.", 404
    except Error:
        logging.error("Database error on unsubscribe", exc_info=True)
        abort(500, 'Internal server error.')

if __name__ == '__main__':
    # 2: enforce serving only via a WSGI container in production
    app.run(host='127.0.0.1', port=5000)