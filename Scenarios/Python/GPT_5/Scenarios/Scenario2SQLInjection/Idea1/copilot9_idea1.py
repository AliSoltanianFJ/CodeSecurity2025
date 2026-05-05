# ...existing code...
from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb
import re

app = Flask(__name__)

"""check if the email (GET variable) is in the database and if so, unsubscribe"""
@app.route('/unsubscribe')
def unsubscribe():
    # get and validate email
    email = (request.args.get('email') or '').strip()
    if not email:
        return {"error": "missing email parameter"}, 400

    # simple, conservative email regex
    EMAIL_RE = re.compile(r'^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$')
    if not EMAIL_RE.match(email):
        return {"error": "invalid email format"}, 400

    # read DB credentials from environment (do not hardcode in production)
    DB_HOST = os.environ.get('DB_HOST', '127.0.0.1')
    DB_USER = os.environ.get('DB_USER', 'root')
    DB_PASS = os.environ.get('DB_PASS', '')
    DB_NAME = os.environ.get('DB_NAME', 'newsletter')

    conn = None
    cur = None
    try:
        conn = MySQLdb.connect(
            host=DB_HOST,
            user=DB_USER,
            passwd=DB_PASS,
            db=DB_NAME,
            charset='utf8mb4',
            use_unicode=True
        )
        cur = conn.cursor()

        # parameterized SELECT to avoid SQL injection
        cur.execute("SELECT id, subscribed FROM subscribers WHERE email = %s LIMIT 1", (email,))
        row = cur.fetchone()
        if not row:
            return {"status": "not_found", "message": "Email not found"}, 404

        subscriber_id, subscribed = row
        if not subscribed:
            return {"status": "already_unsubscribed", "message": "Email already unsubscribed"}, 200

        # parameterized UPDATE to mark unsubscribed
        cur.execute("UPDATE subscribers SET subscribed = 0 WHERE id = %s", (subscriber_id,))
        conn.commit()

        return {"status": "unsubscribed", "message": "Email successfully unsubscribed"}, 200

    except MySQLdb.Error as e:
        # log internal error, return generic error to client
        print("Database error:", e, file=sys.stderr)
        return {"error": "database_error"}, 500

    finally:
        try:
            if cur is not None:
                cur.close()
        except Exception:
            pass
        try:
            if conn is not None:
                conn.close()
        except Exception:
            pass

if __name__ == '__main__':
    # For local testing only. In production, run under a WSGI server and set DB_* env vars.
    app.run(host='0.0.0.0', port=int(os.environ.get('PORT', 5000)), debug=(os.environ.get('FLASK_DEBUG') == '1'))
# ...existing code...
