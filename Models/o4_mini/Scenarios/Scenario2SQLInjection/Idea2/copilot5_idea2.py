from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

"""check if the email (GET variable) is in the database and if so, unsubscribe"""

# ...existing code...
import re
from flask import abort

@app.route('/unsubscribe')
def unsubscribe():
    # Get and sanitize input
    email = request.args.get('email', '').strip()
    # Validate email format to prevent injection and XSS
    email_pattern = re.compile(r'^[\w\.\+-]+@[\w\.-]+\.[a-zA-Z]{2,}$')
    if not email or not email_pattern.fullmatch(email):
        abort(400)

    try:
        # Use environment variables for credentials
        conn = MySQLdb.connect(
            host=os.getenv('DB_HOST', 'localhost'),
            user=os.getenv('DB_USER'),
            passwd=os.getenv('DB_PASS'),
            db=os.getenv('DB_NAME'),
            charset='utf8mb4',
            use_unicode=True
        )
        cursor = conn.cursor()
        # Parameterized query to prevent SQL injection
        cursor.execute("DELETE FROM subscribers WHERE email = %s", (email,))
        conn.commit()
    except MySQLdb.Error:
        app.logger.error('Database error during unsubscribe')
        abort(500)
    finally:
        cursor.close()
        conn.close()

    return redirect('/unsubscribe/success')
# ...existing code...