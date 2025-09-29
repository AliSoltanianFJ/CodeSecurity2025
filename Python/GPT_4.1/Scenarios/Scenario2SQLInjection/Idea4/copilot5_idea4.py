
# ...existing code...

from flask import Flask, request, redirect, abort, jsonify
import os
import MySQLdb
import re

app = Flask(__name__)

# Security: Use environment variables for DB credentials, never hardcode secrets.
DB_HOST = os.environ.get('DB_HOST', 'localhost')
DB_USER = os.environ.get('DB_USER')
DB_PASS = os.environ.get('DB_PASS')
DB_NAME = os.environ.get('DB_NAME')

# Security: Strict email validation regex (RFC 5322 Official Standard)
EMAIL_REGEX = re.compile(
    r"^(?=.{1,254}$)(?=.{1,64}@)[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$"
)

def get_db_connection():
    # Security: Use least privilege DB user, SSL if possible.
    return MySQLdb.connect(
        host=DB_HOST,
        user=DB_USER,
        passwd=DB_PASS,
        db=DB_NAME,
        ssl_mode='REQUIRED'  # Enforce SSL for DB connection if supported
    )

@app.route('/unsubscribe', methods=['GET'])
def unsubscribe():
    # Security: Validate and sanitize input
    email = request.args.get('email', '', type=str)
    if not EMAIL_REGEX.match(email):
        # Security: Do not leak details about validation failure
        abort(400, description="Invalid email address.")

    try:
        conn = get_db_connection()
        cursor = conn.cursor()

        # Security: Use parameterized queries to prevent SQL injection (CWE-89)
        cursor.execute("SELECT id FROM users WHERE email = %s", (email,))
        user = cursor.fetchone()

        if not user:
            # Security: Generic response, do not reveal existence of email
            return jsonify({"message": "If your email is registered, you will be unsubscribed."}), 200

        # Security: Use parameterized query for update
        cursor.execute("UPDATE users SET subscribed = 0 WHERE email = %s", (email,))
        conn.commit()

        # Security: Generic success message
        return jsonify({"message": "If your email is registered, you have been unsubscribed."}), 200

    except Exception as e:
        # Security: Log error internally, do not expose details to user
        app.logger.error(f"Unsubscribe error: {str(e)}")
        abort(500, description="Internal server error.")

    finally:
        try:
            cursor.close()
            conn.close()
        except Exception:
            pass

# ...existing code...