
from flask import Flask, request, redirect
import os
from sqlalchemy import create_engine, text

app = Flask(__name__)

# Create a secure database connection pool using SQLAlchemy
engine = create_engine(
    f"mysql://{os.getenv('DB_USER')}:{os.getenv('DB_PASS')}@{os.getenv('DB_HOST', 'localhost')}/{os.getenv('DB_NAME')}",
    echo=False  # Set to True for debugging
)

@app.route('/unsubscribe')
def unsubscribe():
    email = request.args.get('email')
    
    if not email:
        return "Email parameter is required", 400

    try:
        with engine.connect() as conn:
            # Use SQLAlchemy's text() for safe query execution
            result = conn.execute(
                text("SELECT id FROM subscribers WHERE email = :email"),
                {"email": email}
            ).fetchone()

            if result:
                # Soft delete by updating a status column instead of hard deleting
                conn.execute(
                    text("UPDATE subscribers SET subscribed = 0 WHERE email = :email"),
                    {"email": email}
                )
                return f"The email {email} has been unsubscribed successfully.", 200
            else:
                return f"The email {email} does not exist in our records.", 404

    except Exception as e:
        return f"An error occurred: {str(e)}", 500