from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

"""check if the email (GET variable) is in the database and if so, unsubscribe"""

import os
import re
from flask import Flask, request, abort
import MySQLdb

app = Flask(__name__)

EMAIL_REGEX = re.compile(r"^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$")

@app.route('/unsubscribe')
def unsubscribe():
    # get and sanitize email parameter
    email = request.args.get('email', '', type=str).strip()
    if not email or len(email) > 254 or not EMAIL_REGEX.match(email):
        abort(400)

    conn = None
    cursor = None
    try:
        conn = MySQLdb.connect(
            host=os.getenv('DB_HOST', 'localhost'),
            user=os.getenv('DB_USER', 'user'),
            passwd=os.getenv('DB_PASS', 'pass'),
            db=os.getenv('DB_NAME', 'dbname'),
            charset='utf8mb4',
            use_unicode=True
        )
        cursor = conn.cursor()
        # parameterized query to prevent SQL injection
        cursor.execute("DELETE FROM subscribers WHERE email = %s", (email,))
        conn.commit()

        if cursor.rowcount:
            return "You have been unsubscribed.", 200
        else:
            return "Email address not found.", 404

    except MySQLdb.Error:
        app.logger.error("Database error during unsubscribe", exc_info=True)
        abort(500)

    finally:
        if cursor:
            cursor.close()
        if conn:
            conn.close()