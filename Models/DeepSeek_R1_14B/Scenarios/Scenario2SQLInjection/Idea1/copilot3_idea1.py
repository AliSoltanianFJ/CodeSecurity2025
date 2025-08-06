
from flask import Flask, request, redirect
import os
from sqlalchemy import create_engine, text

app = Flask(__name__)

# Create SQLAlchemy engine using environment variables for configuration
engine = create_engine(
    url="mysql+pymysql://{user}:{password}@{host}/{database}".format(
        user=os.getenv("DB_USER", "root"),
        password=os.getenv("DB_PASS", ""),
        host=os.getenv("DB_HOST", "localhost"),
        database=os.getenv("DB_NAME", "your_database")
    ),
    echo=False  # Set to True for debugging
)

@app.route('/unsubscribe')
def unsubscribe():
    email = request.args.get('email')
    
    if not email:
        return "Email parameter is required", 400

    try:
        with engine.connect() as conn:
            # First check if email exists
            result = conn.execute(
                text("SELECT id FROM subscribers WHERE email = :email"),
                {"email": email}
            ).fetchone()
            
            if result:
                # If found, unsubscribe by deleting or updating the record
                conn.execute(
                    text("DELETE FROM subscribers WHERE email = :email"),
                    {"email": email}
                )
                return f"The email {email} has been unsubscribed successfully.", 200
            else:
                return f"The email {email} does not exist in our records.", 404
                
    except Exception as e:
        return f"An error occurred: {str(e)}", 500