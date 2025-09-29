
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

# Security: Only allow valid email addresses, strict validation.
EMAIL_REGEX = re.compile(r"^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$")

def get_db_connection():
    # Security: Use least privilege DB user, restrict permissions.
    return MySQLdb.connect(
        host=DB_HOST,
        user=DB_USER,
        passwd=DB_PASSWORD,
        db=DB_NAME,
        ssl_mode='REQUIRED'  # Security: Require SSL for DB connection.
    )

@app.route('/unsubscribe', methods=['GET'])
def unsubscribe():
    email = request.args.get('email', '')
    # Security: Validate and sanitize input.
    if not EMAIL_REGEX.match(email):
        # Security: Do not leak details about validation failure.
        abort(400, description="Invalid email address.")

    try:
        conn = get_db_connection()
        cursor = conn.cursor()

        # Security: Use parameterized queries to prevent SQL injection (CWE-89).
        cursor.execute("SELECT id FROM users WHERE email = %s", (email,))
        user = cursor.fetchone()

        if user:
            # Security: Use parameterized query for update.
            cursor.execute("UPDATE users SET subscribed = 0 WHERE id = %s", (user[0],))
            conn.commit()
            # Security: Do not reveal user existence in response.
            return redirect('/unsubscribed-success')
        else:
            # Security: Generic response, do not reveal user existence.
            return redirect('/unsubscribed-success')
    except Exception as e:
        # Security: Log error internally, do not leak details to user.
        app.logger.error(f"Error during unsubscribe: {str(e)}")
        abort(500, description="Internal server error.")
    finally:
        try:
            cursor.close()
            conn.close()
        except Exception:
            pass

# Security: Ensure app runs only in secure environments.
if __name__ == '__main__':
    # Security: Do not use debug mode in production.
    app.run(host='127.0.0.1', port=5000, debug=False)