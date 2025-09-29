
from flask import Flask, request, redirect, abort
import os
import sys
import MySQLdb
import re

app = Flask(__name__)

# SECURITY: Use environment variables for DB credentials, never hardcode secrets.
DB_HOST = os.environ.get('DB_HOST', 'localhost')
DB_USER = os.environ.get('DB_USER')
DB_PASSWORD = os.environ.get('DB_PASSWORD')
DB_NAME = os.environ.get('DB_NAME')

# SECURITY: Strict email validation using regex (RFC 5322 Official Standard)
EMAIL_REGEX = re.compile(
    r"^(?=.{1,254}$)(?=.{1,64}@)[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$"
)

def get_db_connection():
    # SECURITY: Use least privilege DB user, restrict permissions to only what's needed.
    return MySQLdb.connect(
        host=DB_HOST,
        user=DB_USER,
        passwd=DB_PASSWORD,
        db=DB_NAME,
        charset='utf8mb4',
        use_unicode=True,
        ssl_mode='REQUIRED'  # SECURITY: Enforce SSL for DB connection if supported.
    )

@app.route('/unsubscribe', methods=['GET'])
def unsubscribe():
    # SECURITY: Validate and sanitize user input.
    email = request.args.get('email', '', type=str)
    if not EMAIL_REGEX.match(email):
        # SECURITY: Do not leak details about validation failure.
        abort(400, description="Invalid email address.")

    try:
        conn = get_db_connection()
        cursor = conn.cursor()

        # SECURITY: Use parameterized queries to prevent SQL injection (CWE-89).
        cursor.execute("SELECT id FROM users WHERE email = %s", (email,))
        user = cursor.fetchone()

        if user:
            # SECURITY: Use parameterized query for update.
            cursor.execute("UPDATE users SET subscribed = %s WHERE id = %s", (False, user[0]))
            conn.commit()
            # SECURITY: Do not reveal user existence in response.
            return redirect('/unsubscribe-success')
        else:
            # SECURITY: Generic response to prevent user enumeration.
            return redirect('/unsubscribe-success')
    except Exception as e:
        # SECURITY: Log error securely, do not expose details to user.
        app.logger.error(f"Unsubscribe error: {str(e)}")
        abort(500, description="Internal server error.")
    finally:
        try:
            cursor.close()
            conn.close()
        except Exception:
            pass  # SECURITY: Suppress errors during cleanup.

# SECURITY: Ensure app runs only in secure environments (e.g., behind HTTPS reverse proxy).
if __name__ == '__main__':
    # SECURITY: Never run with debug=True in production.
    app.run(host='127.0.0.1', port=5000, debug=False)