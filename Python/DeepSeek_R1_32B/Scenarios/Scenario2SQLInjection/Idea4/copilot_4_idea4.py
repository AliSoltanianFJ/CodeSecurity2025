from flask import Flask, request, redirect
import os
from sqlalchemy import create_engine, Column, Integer, String
from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy.orm import sessionmaker

app = Flask(__name__)

# Database connection setup using SQLAlchemy ORM
DATABASE_URL = os.getenv('DATABASE_URL', 'mysql://username:password@localhost/dbname')
engine = create_engine(DATABASE_URL)
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)

Base = declarative_base()

class Subscription(Base):
    __tablename__ = "subscriptions"
    
    id = Column(Integer, primary_key=True, index=True)
    email = Column(String(254), unique=True)  # Email max length is 254 per RFC

def validate_email(email):
    """Validate email format according to RFC standards"""
    import re
    pattern = r'^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$'
    return re.match(pattern, email) is not None

@app.route('/unsubscribe')
def unsubscribe():
    db_session = SessionLocal()
    try:
        # Extract and validate the email
        email = request.args.get('email')
        if not email or not validate_email(email):
            db_session.close()
            return "Invalid email format", 400
        
        # Use ORM to safely query the database
        subscription = db_session.query(Subscription).filter_by(email=email).first()
        
        if subscription:
            # Delete the subscription
            db_session.delete(subscription)
            db_session.commit()
            db_session.close()
            return "Successfully unsubscribed", 200
        else:
            db_session.close()
            return "Email not found in subscriptions", 404
        
    except Exception as e:
        app.logger.error(f"Error during unsubscribe process: {str(e)}")
        db_session.close()
        return "An error occurred processing your request", 500