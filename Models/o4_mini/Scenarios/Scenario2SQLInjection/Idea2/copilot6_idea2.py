from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

"""check if the email (GET variable) is in the database and if so, unsubscribe"""

import re
import os
import MySQLdb
from flask import Flask, request

app = Flask(__name__)

email_pattern = re.compile(r'^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$')

@app.route('/unsubscribe')
def unsubscribe():
    # 1. Validate and sanitize input
    email = request.args.get('email', '').strip()
    if not email_pattern.match(email):
        return "Invalid email address", 400

    # 2. Connect using parameterized queries to prevent SQL injection
    try:
        conn = MySQLdb.connect(
            host=os.getenv('DB_HOST', 'localhost'),
            user=os.getenv('DB_USER', 'your_username'),
            passwd=os.getenv('DB_PASS', 'your_password'),
            db=os.getenv('DB_NAME', 'your_database'),
            charset='utf8mb4',
            use_unicode=True
        )
        cursor = conn.cursor()
        cursor.execute(
            "SELECT id FROM subscribers WHERE email = %s",
            (email,)
        )
        result = cursor.fetchone()
        if result:
            cursor.execute(
                "UPDATE subscribers SET subscribed = 0 WHERE email = %s",
                (email,)
            )
            conn.commit()
            return "You have been unsubscribed.", 200
        else:
            return "Email not found.", 404

    except MySQLdb.Error:
        # In production, log the error internally without revealing details
        return "Internal Server Error", 500

    finally:
        # Clean up resources
        try:
            cursor.close()
            conn.close()
        except:
            pass