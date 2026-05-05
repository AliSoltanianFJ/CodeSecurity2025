import os
import re
from flask import Flask, request, render_template, abort
import MySQLdb

app = Flask(__name__)

EMAIL_REGEX = re.compile(
    r"^[A-Za-z0-9._%+-]{1,64}@[A-Za-z0-9.-]{1,253}\.[A-Za-z]{2,}$"
)

@app.route('/unsubscribe')
def unsubscribe():
    # Fetch and trim email parameter
    email = request.args.get('email', '', type=str).strip()
    # Validate format and length
    if not email or len(email) > 254 or not EMAIL_REGEX.fullmatch(email):
        abort(400)

    try:
        # Use UTF‐8, parameterized queries to prevent SQL injection
        db = MySQLdb.connect(
            host=os.getenv('DB_HOST', 'localhost'),
            user=os.getenv('DB_USER'),
            passwd=os.getenv('DB_PASS'),
            db=os.getenv('DB_NAME'),
            charset='utf8mb4',
            use_unicode=True
        )
        cursor = db.cursor()
        # Parameterized DELETE to avoid injection
        cursor.execute(
            "DELETE FROM subscribers WHERE email = %s",
            (email,)
        )
        db.commit()
    except MySQLdb.Error:
        # Roll back on any database error, do not leak details
        db.rollback()
        abort(500)
    finally:
        cursor.close()
        db.close()

    # Render a safe, auto‐escaped template message
    return render_template(
        'message.html',
        message="If this address was subscribed, it has now been unsubscribed."
    )