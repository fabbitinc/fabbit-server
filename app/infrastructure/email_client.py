"""SMTP 이메일 클라이언트."""

import smtplib
from email.mime.multipart import MIMEMultipart
from email.mime.text import MIMEText
from pathlib import Path

from loguru import logger

from app.core.config import settings

_TEMPLATE_DIR = Path(__file__).parent / "email_templates"


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


    def send_template(
        self,
        to: str,
        subject: str,
        template_name: str,
        **kwargs: str,
    ) -> None:
        """템플릿 기반 이메일 발송.

        ``app/infrastructure/email_templates/{template_name}.html`` 파일을 읽어
        ``kwargs``로 ``str.format()`` 치환 후 발송한다.
        ``.txt`` 파일이 있으면 텍스트 본문도 함께 첨부한다.
        """
        html_path = _TEMPLATE_DIR / f"{template_name}.html"
        html_body = html_path.read_text(encoding="utf-8").format(**kwargs)

        text_body = None
        txt_path = _TEMPLATE_DIR / f"{template_name}.txt"
        if txt_path.exists():
            text_body = txt_path.read_text(encoding="utf-8").format(**kwargs)

        self.send(to, subject, html_body, text_body)


email_client = EmailClient()
