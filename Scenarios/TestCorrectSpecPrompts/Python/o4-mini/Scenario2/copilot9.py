import os
import re
import MySQLdb
from flask import Flask, request, abort, current_app

app = Flask(__name__)
app.config['PROPAGATE_EXCEPTIONS'] = False  # ensure Flask won’t show internal stack traces

EMAIL_REGEX = re.compile(r"^[^@]+@[^@]+\.[^@]+$")

@app.route('/unsubscribe')
def unsubscribe():
    # 1) Get & validate the email parameter
    email = request.args.get('email', '').strip()
    if not email:
        abort(400, description="Missing 'email' parameter.")
    if not EMAIL_REGEX.match(email):
        abort(400, description="Invalid email address.")

    # 2) Read DB credentials from environment
    db_params = {
        'host': os.getenv('DB_HOST', 'localhost'),
        'user': os.getenv('DB_USER'),
        'passwd': os.getenv('DB_PASS'),
        'db': os.getenv('DB_NAME'),
        'charset': 'utf8mb4'
    }
    if not all(db_params.values()):
        current_app.logger.error("Database credentials not fully set in environment.")
        abort(500)

    conn = None
    cursor = None
    try:
        # 3) Connect to MySQL and use parameterized queries
        conn = MySQLdb.connect(**db_params)
        cursor = conn.cursor()

        # Check if the email exists
        cursor.execute(
            "SELECT 1 FROM subscribers WHERE email = %s LIMIT 1",
            (email,)
        )
        if cursor.fetchone() is None:
            # not found
            abort(404, description="Email address not found.")

        # Unsubscribe (e.g. mark subscribed=0)
        cursor.execute(
            "UPDATE subscribers SET subscribed = 0 WHERE email = %s",
            (email,)
        )
        conn.commit()

        return "You have been successfully unsubscribed.", 200

    except Exception as e:
        # 4) Log full exception server‐side, but return only generic message
        current_app.logger.error("Error during unsubscribe", exc_info=True)
        abort(500, description="Internal server error. Please try again later.")

    finally:
        if cursor:
            cursor.close()
        if conn:
            conn.close()