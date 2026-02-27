"""SMTP 이메일 클라이언트."""

import smtplib
from email.mime.multipart import MIMEMultipart
from email.mime.text import MIMEText

from loguru import logger

from app.core.config import settings


class EmailClient:
    """SMTP 이메일 발송 클라이언트.

    모듈 하단의 ``email_client`` 인스턴스를 import 하여 사용하세요.
    """

    def __init__(self) -> None:
        self._host = settings.smtp_host
        self._port = settings.smtp_port
        self._username = settings.smtp_username
        self._password = settings.smtp_password
        self._use_tls = settings.smtp_use_tls
        self._from_email = settings.smtp_from_email
        self._from_name = settings.smtp_from_name

    def send(
        self,
        to: str,
        subject: str,
        html_body: str,
        text_body: str | None = None,
    ) -> None:
        """이메일 발송."""
        msg = MIMEMultipart("alternative")
        msg["From"] = f"{self._from_name} <{self._from_email}>"
        msg["To"] = to
        msg["Subject"] = subject

        if text_body:
            msg.attach(MIMEText(text_body, "plain", "utf-8"))
        msg.attach(MIMEText(html_body, "html", "utf-8"))

        with smtplib.SMTP(self._host, self._port) as server:
            if self._use_tls:
                server.starttls()
            if self._username:
                server.login(self._username, self._password)
            server.sendmail(self._from_email, to, msg.as_string())

        logger.info("이메일 발송 완료: to={to} subject={subject}", to=to, subject=subject)


email_client = EmailClient()
