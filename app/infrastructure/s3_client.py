"""S3 호환 스토리지 클라이언트 (Cloudflare R2 / MinIO)."""

import time

import boto3
from botocore.config import Config
from loguru import logger

from app.core.config import settings


class S3Client:
    """S3 호환 스토리지 클라이언트.

    모듈 하단의 ``s3_client`` 인스턴스를 import 하여 사용하세요.
    """

    def __init__(self) -> None:
        self._client = boto3.client(
            "s3",
            endpoint_url=settings.storage_endpoint,
            aws_access_key_id=settings.storage_access_key,
            aws_secret_access_key=settings.storage_secret_key,
            region_name="auto",
            config=Config(s3={"addressing_style": "path"}),
        )
        self._bucket = settings.storage_bucket
        self._public_url = settings.storage_public_url

    def _get_file_url(self, file_key: str) -> str:
        """파일 공개 URL 생성."""
        if self._public_url:
            return f"{self._public_url.rstrip('/')}/{file_key}"
        return f"{settings.storage_endpoint}/{self._bucket}/{file_key}"

    def get_file_url(self, file_key: str | None) -> str | None:
        """파일 공개 URL 생성 (None-safe). key가 없으면 None 반환."""
        if not file_key:
            return None
        return self._get_file_url(file_key)

    def generate_upload_presigned_url(
        self,
        file_key: str,
        content_type: str,
        content_length: int,
        expiration_minutes: int = 15,
    ) -> dict[str, str]:
        """업로드용 Presigned URL 생성.

        Returns:
            {"upload_url": "...", "file_url": "...", "file_key": "..."}
        """
        upload_url = self._client.generate_presigned_url(
            "put_object",
            Params={
                "Bucket": self._bucket,
                "Key": file_key,
                "ContentType": content_type,
                "ContentLength": content_length,
            },
            ExpiresIn=expiration_minutes * 60,
        )
        return {
            "upload_url": upload_url,
            "file_url": self._get_file_url(file_key),
            "file_key": file_key,
        }

    def generate_download_presigned_url(
        self,
        file_key: str,
        expiration_minutes: int = 5,
    ) -> str:
        """다운로드용 Presigned URL 생성."""
        return self._client.generate_presigned_url(
            "get_object",
            Params={"Bucket": self._bucket, "Key": file_key},
            ExpiresIn=expiration_minutes * 60,
        )

    def head_object(self, file_key: str) -> dict | None:
        """S3 객체 메타데이터 조회 (존재 확인 + 크기 반환).

        Returns:
            {"content_length": int, "content_type": str} 또는 존재하지 않으면 None
        """
        try:
            resp = self._client.head_object(Bucket=self._bucket, Key=file_key)
            return {
                "content_length": resp["ContentLength"],
                "content_type": resp["ContentType"],
            }
        except self._client.exceptions.ClientError as e:
            if e.response["Error"]["Code"] == "404":
                return None
            raise

    def get_object(self, file_key: str) -> bytes:
        """S3 객체 다운로드 → 바이트 반환."""
        t0 = time.perf_counter()
        resp = self._client.get_object(Bucket=self._bucket, Key=file_key)
        data = resp["Body"].read()
        elapsed = time.perf_counter() - t0
        logger.info("[S3] 다운로드: {key} ({size} bytes, {elapsed:.1f}s)", key=file_key, size=len(data), elapsed=elapsed)
        return data

    def put_object(self, file_key: str, data: bytes, content_type: str) -> int:
        """S3에 바이트 데이터 업로드. 크기 반환."""
        t0 = time.perf_counter()
        self._client.put_object(
            Bucket=self._bucket,
            Key=file_key,
            Body=data,
            ContentType=content_type,
        )
        elapsed = time.perf_counter() - t0
        logger.info(
            "[S3] 업로드: {key} ({size} bytes, {elapsed:.1f}s)",
            key=file_key,
            size=len(data),
            elapsed=elapsed,
        )
        return len(data)

    def delete_object(self, file_key: str) -> None:
        """S3 객체 삭제."""
        self._client.delete_object(Bucket=self._bucket, Key=file_key)

    def list_keys(
        self, prefix: str, max_keys: int = 1000
    ) -> list[str]:
        """prefix 하위의 모든 S3 키를 페이지네이션으로 수집."""
        keys: list[str] = []
        params = {"Bucket": self._bucket, "Prefix": prefix, "MaxKeys": max_keys}
        while True:
            resp = self._client.list_objects_v2(**params)
            for obj in resp.get("Contents", []):
                keys.append(obj["Key"])
            if not resp.get("IsTruncated"):
                break
            params["ContinuationToken"] = resp["NextContinuationToken"]
        return keys


s3_client = S3Client()
