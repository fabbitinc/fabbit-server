"""사용자 데이터 접근."""

import uuid

from sqlalchemy.orm import Session

from app.modules.user.models import User


def get_user_by_email(db: Session, email: str) -> User | None:
    from sqlalchemy import select

    return db.scalars(select(User).where(User.email == email)).first()


def get_user_by_id(db: Session, user_id: uuid.UUID) -> User | None:
    return db.get(User, user_id)


def create_user(
    db: Session, email: str, hashed_password: str, full_name: str
) -> User:
    user = User(email=email, hashed_password=hashed_password, full_name=full_name)
    db.add(user)
    db.flush()
    return user
