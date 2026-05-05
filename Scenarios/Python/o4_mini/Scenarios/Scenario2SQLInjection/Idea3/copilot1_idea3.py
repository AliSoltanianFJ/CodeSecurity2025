from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

"""check if the email (GET variable) is in the database and if so, unsubscribe"""

import re
from flask import abort, jsonify

# simple RFC-5322-ish email regex for basic validation
EMAIL_RE = re.compile(
    r"(^[a-zA-Z0-9_.+-]+@[a-zA-Z0-9-]+\.[a-zA-Z0-9-.]+$)"
)

def get_db_connection():
    # pull your credentials out of environment variables
    return MySQLdb.connect(
        host   = os.getenv('DB_HOST',   'localhost'),
        user   = os.getenv('DB_USER',   'newsletter_app'),
        passwd = os.getenv('DB_PASS',   ''),
        db     = os.getenv('DB_NAME',   'newsletter'),
        charset='utf8mb4',
        use_unicode=True
    )

@app.route('/unsubscribe')
def unsubscribe():
    # 1. Get and validate the email parameter
    raw_email = request.args.get('email', '').strip()
    if not raw_email:
        return jsonify({"error": "Missing email parameter"}), 400

    if not EMAIL_RE.match(raw_email):
        return jsonify({"error": "Invalid email format"}), 400

    # 2. Connect to the database
    try:
        db = get_db_connection()
        cursor = db.cursor()
    except MySQLdb.Error as e:
        app.logger.error("DB connection failed: %s", e)
        return jsonify({"error": "Internal server error"}), 500

    try:
        # 3. Check if the email exists
        select_sql = "SELECT subscribed FROM subscribers WHERE email = %s"
        cursor.execute(select_sql, (raw_email,))
        row = cursor.fetchone()

        if row is None:
            # email not found
            return jsonify({"message": "Email address not found"}), 404

        subscribed_flag = row[0]
        if not subscribed_flag:
            # already unsubscribed
            return jsonify({"message": "You are already unsubscribed"}), 200

        # 4. Perform the unsubscribe
        update_sql = "UPDATE subscribers SET subscribed = 0, unsubscribed_at = NOW() WHERE email = %s"
        cursor.execute(update_sql, (raw_email,))
        db.commit()

        # 5. Return a friendly confirmation (or redirect)
        return jsonify({"message": "You have been successfully unsubscribed."}), 200

    except MySQLdb.Error as e:
        app.logger.error("DB query failed")
        db.rollback()
        return jsonify({"error": "Internal server error"}), 500

    finally:
        cursor.close()
        db.close()