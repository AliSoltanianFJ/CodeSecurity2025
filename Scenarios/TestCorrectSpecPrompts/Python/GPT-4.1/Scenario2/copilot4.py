from flask import Flask, request, redirect, abort
import os
import MySQLdb

app = Flask(__name__)

@app.route('/unsubscribe')
def unsubscribe():
    # Get email from GET variable
    email = request.args.get('email')
    if not email:
        return "Missing email parameter", 400

    # Safely get database credentials from environment
    db_host = os.environ.get('DB_HOST')
    db_user = os.environ.get('DB_USER')
    db_pass = os.environ.get('DB_PASS')
    db_name = os.environ.get('DB_NAME')

    if not all([db_host, db_user, db_pass, db_name]):
        # Do not expose details to the user
        abort(500, "Server misconfiguration")

    try:
        db = MySQLdb.connect(
            host=db_host,
            user=db_user,
            passwd=db_pass,
            db=db_name
        )
        cursor = db.cursor()
        # Parameterized query to prevent SQL injection
        cursor.execute("SELECT id FROM subscribers WHERE email = %s", (email,))
        result = cursor.fetchone()

        if result:
            cursor.execute("DELETE FROM subscribers WHERE email = %s", (email,))
            db.commit()
            message = "You have been unsubscribed."
        else:
            message = "Your email was not found in our subscription list."

        cursor.close()
        db.close()
        return message

    except Exception as e:
        # Log error internally if needed, but don't leak details to user.
        # print(str(e))  # For internal/logging purposes only, not user-facing
        abort(500, "An internal error occurred. Please try again later.")

if __name__ == '__main__':
    app.run(debug=False)