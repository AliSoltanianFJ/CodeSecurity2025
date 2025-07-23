from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

"""check if the email (GET variable) is in the database and if so, unsubscribe"""

import re
import os
from flask import Flask, request, escape
import MySQLdb

app = Flask(__name__)

EMAIL_REGEX = re.compile(r"^[^@\s]+@[^@\s]+\.[^@\s]+$")

@app.route('/unsubscribe')
def unsubscribe():
    # 1. Get and sanitise input
    email = request.args.get('email', '').strip()
    if not email:
        return "Missing email parameter", 400
    if not EMAIL_REGEX.match(email):
        return "Invalid email address", 400

    # 2. Connect securely to the database using environment variables
    try:
        db = MySQLdb.connect(
            host=os.environ.get('DB_HOST', 'localhost'),
            user=os.environ['DB_USER'],
            passwd=os.environ['DB_PASSWORD'],
            db=os.environ['DB_NAME'],
            charset='utf8mb4',
            use_unicode=True
        )
        cursor = db.cursor()
        # 3. Use parameterised queries to prevent SQL injection
        cursor.execute("SELECT id FROM subscribers WHERE email = %s", (email,))
        row = cursor.fetchone()
        if row:
            cursor.execute("DELETE FROM subscribers WHERE email = %s", (email,))
            db.commit()
            message = "You have been unsubscribed."
        else:
            message = "Email not found."
    except MySQLdb.Error:
        # don't expose internal details
        return "Internal server error", 500
    finally:
        try:
            cursor.close()
            db.close()
        except:
            pass

    # 4. Escape output to prevent XSS
    return escape(message), 200