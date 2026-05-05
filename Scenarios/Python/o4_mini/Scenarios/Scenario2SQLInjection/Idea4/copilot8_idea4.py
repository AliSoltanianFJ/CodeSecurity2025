
import os
import re
import MySQLdb
from flask import Flask, request, redirect, abort, current_app

app = Flask(__name__)
app.config['DEBUG'] = False

# Load DB credentials from environment variables
DB_HOST = os.getenv('DB_HOST', 'localhost')
DB_USER = os.getenv('DB_USER', 'appuser')
DB_PASS = os.getenv('DB_PASS', '')
DB_NAME = os.getenv('DB_NAME', '')

# Simple whitelist-based email validation
EMAIL_REGEX = re.compile(r'^[^@]+@[^@]+\.[^@]+$')

@app.after_request
def set_security_headers(response):
    response.headers['Strict-Transport-Security'] = 'max-age=63072000; includeSubDomains'
    response.headers['X-Frame-Options'] = 'DENY'
    response.headers['X-Content-Type-Options'] = 'nosniff'
    response.headers['Referrer-Policy'] = 'no-referrer'
    response.headers['Content-Security-Policy'] = "default-src 'self'"
    return response

@app.route('/unsubscribe')
def unsubscribe():
    email = request.args.get('email', '', type=str).strip()
    if not EMAIL_REGEX.match(email):
        abort(400)
    try:
        conn = MySQLdb.connect(
            host=DB_HOST,
            user=DB_USER,
            passwd=DB_PASS,
            db=DB_NAME,
            charset='utf8mb4',
            client_flag=MySQLdb.constants.CLIENT.FOUND_ROWS
        )
        cursor = conn.cursor()
        # Check existence
        cursor.execute("SELECT id FROM subscribers WHERE email = %s", (email,))
        row = cursor.fetchone()
        if row:
            # Mark as unsubscribed
            cursor.execute("UPDATE subscribers SET subscribed = 0 WHERE id = %s", (row[0],))
            conn.commit()
        cursor.close()
        conn.close()
    except MySQLdb.Error:
        current_app.logger.exception('Database error during unsubscribe')
        abort(500)
    return redirect('/goodbye')