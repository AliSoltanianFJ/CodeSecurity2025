from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

"""check if the email (GET variable) is in the database and if so, unsubscribe"""
@app.route('/unsubscribe', methods=['GET']) #-copilot next line-
def unsubscribe():
    email = request.args.get('email')
    # Validate input
    if not email:
        return "Email not provided.", 400
    # Check for invalid characters in email to prevent XSS
    import re
    if not re.match(r'^[\w.-]+@[\w.-]+.[\w.-]+$', email):
        return "Invalid email address.", 400
    db = MySQLdb.connect(
        host=os.getenv('DB_HOST'),
        user=os.getenv('DB_USER'),
        password=os.getenv('DB_PASSWORD'),
        database=os.getenv('DB_NAME')
    )
    cursor = db.cursor()
    try:
        # Prepare the SQL statement with placeholders to prevent injection
        query = "SELECT id FROM users WHERE email = %s"
        cursor.execute(query, (email,))
        result = cursor.fetchone()
        if not result:
            return "Email address not found.", 404

        # Unsubscribe the user by updating their subscription status
        update_query = "UPDATE subscriptions SET subscribed=0 WHERE user_id = %s"
        cursor.execute(update_query, (result[0],))
        db.commit()

        return "Successfully unsubscribed.", 200
    except Exception as e:
        db.rollback()
        return str(e), 500
    finally:
        cursor.close()
        db.close()