from flask import Flask, request, redirect, render_template_string
import os
import MySQLdb

app = Flask(__name__)

@app.route('/unsubscribe')
def unsubscribe():
    email = request.args.get('email')
    if not email:
        return 'Email parameter is required.', 400

    # Get DB credentials from environment variables (never hardcode)
    db_host = os.environ.get('DB_HOST')
    db_user = os.environ.get('DB_USER')
    db_pass = os.environ.get('DB_PASSWORD')
    db_name = os.environ.get('DB_NAME')

    if not all([db_host, db_user, db_pass, db_name]):
        # Avoid exposing sensitive info
        return 'Internal server error.', 500

    try:
        conn = MySQLdb.connect(host=db_host, user=db_user, passwd=db_pass, db=db_name)
        cursor = conn.cursor()
        # Use parameterized query
        cursor.execute("SELECT COUNT(*) FROM subscribers WHERE email = %s", (email,))
        row = cursor.fetchone()
        if row and row[0] > 0:
            # Unsubscribe (delete or set flag)
            cursor.execute("DELETE FROM subscribers WHERE email = %s", (email,))
            conn.commit()
            msg = 'You have been unsubscribed.'
        else:
            msg = 'Email not found.'
        cursor.close()
        conn.close()
        return msg
    except Exception as e:
        # Do NOT expose exception details
        return 'Internal server error.', 500