
from flask import Flask, request, redirect, abort
import os
import MySQLdb
import re

app = Flask(__name__)

# SECURITY: Never hardcode credentials. Use environment variables or secure vaults.
DB_HOST = os.environ.get('DB_HOST', 'localhost')
DB_USER = os.environ.get('DB_USER')
DB_PASSWORD = os.environ.get('DB_PASSWORD')
DB_NAME = os.environ.get('DB_NAME')

# SECURITY: Only allow valid email addresses using strict regex.
EMAIL_REGEX = re.compile(
    r"^(?=.{1,254}$)(?=.{1,64}@)[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$"
)

def get_db_connection():
    # SECURITY: Use least privilege for DB user, restrict access to only necessary tables.
    return MySQLdb.connect(
        host=DB_HOST,
        user=DB_USER,
        passwd=DB_PASSWORD,
        db=DB_NAME,
        charset='utf8mb4',
        use_unicode=True,
        ssl_mode='REQUIRED'  # SECURITY: Enforce encrypted connection
    )

@app.route('/unsubscribe', methods=['GET'])
def unsubscribe():
    email = request.args.get('email', '', type=str)

    # SECURITY: Validate and sanitize email input.
    if not EMAIL_REGEX.match(email):
        # SECURITY: Do not leak details about validation failure.
        abort(400, description="Invalid email address.")

    try:
        conn = get_db_connection()
        cursor = conn.cursor()

        # SECURITY: Use parameterized queries to prevent SQL injection (CWE-89).
        cursor.execute("SELECT id FROM subscribers WHERE email = %s", (email,))
        result = cursor.fetchone()

        if result:
            # SECURITY: Use parameterized query for deletion.
            cursor.execute("DELETE FROM subscribers WHERE email = %s", (email,))
            conn.commit()
            # SECURITY: Do not reveal sensitive info in response.
            return redirect('/unsubscribed-success')
        else:
            # SECURITY: Generic response to avoid user enumeration.
            return redirect('/unsubscribed-success')
    except Exception as e:
        # SECURITY: Log error securely, do not expose details to user.
        app.logger.error(f"Unsubscribe error: {str(e)}")
        abort(500, description="Internal server error.")
    finally:
        try:
            cursor.close()
            conn.close()
        except Exception:
            pass

# SECURITY: Ensure app runs only in secure environments.
if __name__ == '__main__':
    # SECURITY: Never run with debug=True in production.
    app.run(host='127.0.0.1', port=5000, debug=False)