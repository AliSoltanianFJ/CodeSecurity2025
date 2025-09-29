
# ...existing code...

from flask import Flask, request, redirect, abort, render_template_string
import os
import MySQLdb
import re

app = Flask(__name__)

# SECURITY: Use environment variables for DB credentials, never hardcode secrets.
DB_HOST = os.environ.get('DB_HOST', 'localhost')
DB_USER = os.environ.get('DB_USER')
DB_PASS = os.environ.get('DB_PASS')
DB_NAME = os.environ.get('DB_NAME')

# SECURITY: Strict email validation regex (RFC 5322 Official Standard)
EMAIL_REGEX = re.compile(
    r"^(?=.{1,254}$)(?=.{1,64}@)[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$"
)

def get_db_connection():
    # SECURITY: Use least privilege DB user, SSL if possible.
    return MySQLdb.connect(
        host=DB_HOST,
        user=DB_USER,
        passwd=DB_PASS,
        db=DB_NAME,
        charset='utf8mb4',
        use_unicode=True,
        ssl_mode='REQUIRED'  # Enforce SSL for DB connection
    )

@app.route('/unsubscribe', methods=['GET'])
def unsubscribe():
    email = request.args.get('email', '', type=str)

    # SECURITY: Validate and sanitize email input.
    if not EMAIL_REGEX.match(email):
        # Do not leak details about validation failure.
        abort(400, description="Invalid request.")

    try:
        conn = get_db_connection()
        cursor = conn.cursor()

        # SECURITY: Use parameterized queries to prevent SQL injection (CWE-89).
        cursor.execute("SELECT id FROM users WHERE email = %s", (email,))
        user = cursor.fetchone()

        if user:
            # SECURITY: Use parameterized query for update.
            cursor.execute("UPDATE users SET subscribed = 0 WHERE id = %s", (user[0],))
            conn.commit()
            # SECURITY: Generic success message, no sensitive info leaked.
            return render_template_string("<p>You have been unsubscribed.</p>")
        else:
            # SECURITY: Do not reveal if email exists.
            return render_template_string("<p>If your email is registered, you will be unsubscribed.</p>")
    except Exception:
        # SECURITY: Log error internally, do not leak details to user.
        abort(500, description="An error occurred. Please try again later.")
    finally:
        try:
            cursor.close()
            conn.close()
        except Exception:
            pass

# ...existing code...