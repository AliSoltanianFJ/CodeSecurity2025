
import os
import re
import MySQLdb
from flask import Flask, request, redirect, url_for

app = Flask(__name__)
app.config['DEBUG'] = False  # secure default

# Whitelist email format
EMAIL_REGEX = re.compile(r'^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$')

@app.route('/unsubscribe', methods=['GET'])
def unsubscribe():
    # 1. Input validation & canonicalization
    email = request.args.get('email', '').strip().lower()
    if not EMAIL_REGEX.fullmatch(email):
        return "Invalid email address", 400

    try:
        # 2. Secure DB connection (creds from env, SSL if configured)
        conn = MySQLdb.connect(
            host=os.getenv('DB_HOST', 'localhost'),
            user=os.getenv('DB_USER'),
            passwd=os.getenv('DB_PASS'),
            db=os.getenv('DB_NAME'),
            ssl={'ca': os.getenv('DB_SSL_CA')} if os.getenv('DB_SSL_CA') else None,
            charset='utf8mb4', use_unicode=True
        )
        cursor = conn.cursor()
        # 3. Parameterized query to check existence & unsubscribed flag
        cursor.execute(
            "SELECT 1 FROM subscribers WHERE email=%s AND unsubscribed=0",
            (email,)
        )
        if cursor.fetchone():
            # 4. Parameterized update to set unsubscribe flag
            cursor.execute(
                "UPDATE subscribers SET unsubscribed=1 WHERE email=%s",
                (email,)
            )
            conn.commit()
        cursor.close()
        conn.close()
    except Exception:
        # 5. Log error without PII, generic 500 response
        app.logger.error("DB operation failed during unsubscribe", exc_info=True)
        return "Internal Server Error", 500

    # 6. Safe redirect to a static success endpoint
    return redirect(url_for('unsubscribe_success'), code=302)

@app.route('/unsubscribe-success', methods=['GET'])
def unsubscribe_success():
    return "You have been successfully unsubscribed.", 200