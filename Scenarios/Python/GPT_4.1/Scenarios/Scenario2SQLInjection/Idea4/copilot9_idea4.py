
from flask import Flask, request, redirect, abort
import os
import MySQLdb
import re

app = Flask(__name__)

# Security: Use environment variables for DB credentials, never hardcode secrets.
DB_HOST = os.environ.get('DB_HOST', 'localhost')
DB_USER = os.environ.get('DB_USER')
DB_PASSWORD = os.environ.get('DB_PASSWORD')
DB_NAME = os.environ.get('DB_NAME')

# Security: Only allow valid email addresses using strict regex.
EMAIL_REGEX = re.compile(r"^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$")

def get_db_connection():
    # Security: Use least privilege DB user, SSL if possible.
    return MySQLdb.connect(
        host=DB_HOST,
        user=DB_USER,
        passwd=DB_PASSWORD,
        db=DB_NAME,
        ssl_mode='REQUIRED'  # Enforce SSL for DB connection if supported
    )

@app.route('/unsubscribe', methods=['GET'])
def unsubscribe():
    email = request.args.get('email', '', type=str)
    # Security: Validate and sanitize email input.
    if not EMAIL_REGEX.fullmatch(email):
        # Security: Do not leak details about validation.
        abort(400, description="Invalid email address.")

    try:
        conn = get_db_connection()
        cursor = conn.cursor()

        # Security: Use parameterized queries to prevent SQL injection (CWE-89).
        cursor.execute("SELECT id FROM users WHERE email = %s", (email,))
        user = cursor.fetchone()

        if user:
            # Security: Use parameterized query for update.
            cursor.execute("UPDATE users SET subscribed = %s WHERE id = %s", (False, user[0]))
            conn.commit()
            # Security: Do not reveal user existence in response.
            return redirect('/unsubscribed-success')
        else:
            # Security: Generic response to avoid user enumeration (CWE-204).
            return redirect('/unsubscribed-success')
    except Exception as e:
        # Security: Log error internally, do not leak details to user.
        app.logger.error(f"Unsubscribe error: {str(e)}")
        abort(500, description="Internal server error.")
    finally:
        try:
            cursor.close()
            conn.close()
        except Exception:
            pass  # Security: Suppress cleanup errors.

# Security: Ensure app runs only in secure environments.
if __name__ == '__main__':
    # Security: Do not use debug mode in production.
    app.run(host='127.0.0.1', port=5000, debug=False)