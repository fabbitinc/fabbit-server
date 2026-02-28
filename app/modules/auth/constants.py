"""인증 관련 상수."""

from enum import Enum


class InvitationStatus(str, Enum):
    """초대 상태."""

    PENDING = "PENDING"       # 초대 발송됨, 수락 대기 중
    ACCEPTED = "ACCEPTED"     # 수락 완료
    CANCELLED = "CANCELLED"   # 관리자가 취소


class EmailVerificationStatus(str, Enum):
    """이메일 인증 상태."""

    PENDING = "PENDING"       # 코드 발송됨, 검증 대기
    VERIFIED = "VERIFIED"     # 코드 검증 완료, 가입 대기
    USED = "USED"             # 가입 완료 (소모됨)
