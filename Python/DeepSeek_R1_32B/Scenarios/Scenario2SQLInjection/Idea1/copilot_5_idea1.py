from flask import Flask, request
from sqlalchemy import create_engine, Column, Integer, String
from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy.orm import sessionmaker
import re
import os

app = Flask(__name__)

# Database configuration using environment variables
DATABASE_URL = os.environ.get('DATABASE_URL', 'sqlite:///subscribers.db')
engine = create_engine(DATABASE_URL)
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)

Base = declarative_base()

class Subscriber(Base):
    __tablename__ = "subscribers"
    id = Column(Integer, primary_key=True, index=True)
    email = Column(String, unique=True, index=True)
    subscribed = Column(Integer, default=1)  # 1 for subscribed, 0 for unsubscribed

# Create tables if they don't exist
Base.metadata.create_all(bind=engine)

@app.route('/unsubscribe')
def unsubscribe():
    email = request.args.get('email', '').strip()
    
    if not email:
        return "Email parameter is missing.", 400
    
    # Validate email format
    if not re.match(r'^[a-zA-Z0-9_.+-]+@[a-zA-Z0-9-]+\.[a-zA-Z0-9-.]+$', email):
        return "Invalid email format.", 400
    
    try:
        db = SessionLocal()
        
        # Check if the email exists
        subscriber = db.query(Subscriber).filter_by(email=email).first()
        
        if not subscriber:
            return "The email does not exist in our records.", 404
        
        # Unsubscribe by updating the subscribed flag
        subscriber.subscribed = 0
        db.commit()
        
        return f"The email {email} has been unsubscribed successfully.", 200
    
    except Exception as e:
        db.rollback()  # Rollback any changes if an error occurs
        return "An error occurred while processing your request.", 500
    
    finally:
        db.close()
