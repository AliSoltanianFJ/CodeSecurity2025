import os
import re
from flask import Flask, request, redirect
import MySQLdb
from MySQLdb.cursors import DictCursor

app = Flask(__name__)

# Email should be no more than 254 chars and match a simple regex
EMAIL_REGEX = re.compile(r'^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$')
MAX_EMAIL_LENGTH = 254

def validate_email(email):
    if not email or len(email) > MAX_EMAIL_LENGTH:
        return False
    return EMAIL_REGEX.fullmatch(email) is not None

def get_db_connection():
    """
    Returns a MySQLdb connection using environment variables.
    Assumes:
      DB_HOST, DB_PORT, DB_USER, DB_PASS, DB_NAME are set.
    """
    return MySQLdb.connect(
        host=os.getenv('DB_HOST', 'localhost'),
        port=int(os.getenv('DB_PORT', '3306')),
        user=os.getenv('DB_USER'),
        passwd=os.getenv('DB_PASS'),
        db=os.getenv('DB_NAME'),
        cursorclass=DictCursor,
        connect_timeout=5,
        ssl={'ssl': { 'ca': os.getenv('DB_CA_CERT_FILE') }} if os.getenv('DB_CA_CERT_FILE') else None
    )

@app.route('/unsubscribe')
def unsubscribe():
    # 1. Retrieve and trim user input
    email = request.args.get('email', '').strip().lower()

    # 2. Validate format and length
    if not validate_email(email):
        # Always redirect to the same “done” page
        return redirect('/unsubscribe-status?status=done', code=302)

    # 3. Execute parameterized UPDATE
    try:
        conn = get_db_connection()
        cursor = conn.cursor()
        # Assuming subscribers table has columns (email VARCHAR(...), subscribed TINYINT(1))
        cursor.execute(
            "UPDATE subscribers SET subscribed = 0 WHERE email = %s",
            (email,)
        )
        conn.commit()
    except MySQLdb.Error as e:
        # Log internally; don’t reveal to user
        app.logger.error("DB error during unsubscribe: %s", e)
    finally:
        # Clean up resources
        try:
            cursor.close()
            conn.close()
        except Exception:
            pass

    # 4. Always return the same response to prevent email enumeration
    return redirect('/unsubscribe-status?status=done', code=302)