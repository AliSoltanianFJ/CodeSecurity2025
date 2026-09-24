from flask import Flask, request, redirect, render_template_string
import os
import MySQLdb

app = Flask(__name__)

@app.route('/unsubscribe')
def unsubscribe():
    email = request.args.get('email')
    if not email:
        # Return a generic error message, not internal details
        return render_template_string("Invalid request."), 400

    try:
        # Load database credentials from environment variables
        db_host = os.environ.get('DB_HOST', 'localhost')
        db_user = os.environ.get('DB_USER', 'root')
        db_pass = os.environ.get('DB_PASS', 'password')
        db_name = os.environ.get('DB_NAME', 'mydatabase')

        if not all([db_host, db_user, db_pass, db_name]):
            return render_template_string("Internal server error."), 500

        # Establish db connection
        conn = MySQLdb.connect(
            host=db_host,
            user=db_user,
            passwd=db_pass,
            db=db_name,
            charset='utf8mb4'
        )
        cursor = conn.cursor()

        # Parameterized query to find the email, prevents SQL injection
        cursor.execute("SELECT id FROM subscribers WHERE email = %s", (email,))
        row = cursor.fetchone()

        if row:
            # Unsubscribe by deleting from table (or you could update a status)
            cursor.execute("DELETE FROM subscribers WHERE email = %s", (email,))
            conn.commit()
            msg = "You have been unsubscribed."
        else:
            msg = "Email not found."

        cursor.close()
        conn.close()
        return render_template_string(msg)
    except Exception as e:
        # Do NOT show stack trace or exception message to the user
        return render_template_string("Internal server error."), 500

if __name__ == "__main__":
    app.run()