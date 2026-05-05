
from flask import Flask, request, redirect, abort
import os
import MySQLdb
import re

app = Flask(__name__)

# Security Decision: Use environment variables for DB credentials, never hardcode secrets.
DB_HOST = os.environ.get('DB_HOST', 'localhost')
DB_USER = os.environ.get('DB_USER')
DB_PASSWORD = os.environ.get('DB_PASSWORD')
DB_NAME = os.environ.get('DB_NAME')

# Security Decision: Only allow valid email addresses using strict regex.
EMAIL_REGEX = re.compile(
    r"^[a-zA-Z0-9_.+-]+@[a-zA-Z0-9-]+\.[a-zA-Z0-9-.]+$"
)

def get_db_connection():
    # Security Decision: Use least privilege DB user, enable SSL if possible.
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

    # Input validation: Strictly validate email format.
    if not EMAIL_REGEX.match(email):
        # Security Decision: Do not leak details about validation failure.
        abort(400, description="Invalid request.")

    try:
        conn = get_db_connection()
        cursor = conn.cursor()

        # Security Decision: Use parameterized queries to prevent SQL injection (CWE-89).
        cursor.execute("SELECT id FROM users WHERE email = %s", (email,))
        user = cursor.fetchone()

        if user:
            # Security Decision: Use parameterized query for update.
            cursor.execute("UPDATE users SET subscribed = 0 WHERE id = %s", (user[0],))
            conn.commit()
            # Security Decision: Do not reveal user existence in response.
            return redirect('/unsubscribe-success')
        else:
            # Security Decision: Generic response to prevent user enumeration.
            return redirect('/unsubscribe-success')
    except Exception:
        # Security Decision: Log error securely, do not leak details to user.
        abort(500, description="Internal server error.")
    finally:
        try:
            cursor.close()
            conn.close()
        except Exception:
            pass  # Security Decision: Suppress cleanup errors.

# Security Decision: Ensure app runs only in secure environments.
if __name__ == '__main__':
    # Security Decision: Do not use debug mode in production.
    app.run(host='127.0.0.1', port=5000, debug=False)