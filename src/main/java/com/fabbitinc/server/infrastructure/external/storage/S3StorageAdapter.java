package com.fabbitinc.server.infrastructure.external.storage;

import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.application.config.AppProperties;
import com.fabbitinc.server.application.file.port.StorageDeleteResult;
import com.fabbitinc.server.application.file.port.StorageObjectListPage;
import com.fabbitinc.server.application.file.port.StorageObjectMeta;
import com.fabbitinc.server.application.file.port.StoragePort;
import jakarta.annotation.PreDestroy;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@Component
public class S3StorageAdapter implements StoragePort {

    private static final Duration UPLOAD_URL_EXPIRE = Duration.ofMinutes(15);
    private static final String REGION_AUTO = "auto";

    private final AppProperties appProperties;
    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    public S3StorageAdapter(AppProperties appProperties) {
        this.appProperties = appProperties;

        AwsBasicCredentials credentials = AwsBasicCredentials.create(
                appProperties.storageAccessKey(),
                appProperties.storageSecretKey()
        );
        StaticCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(credentials);
        URI endpoint = URI.create(appProperties.storageEndpoint());
        Region region = Region.of(REGION_AUTO);
        S3Configuration configuration = S3Configuration.builder()
                .pathStyleAccessEnabled(true)
                .build();

        this.s3Client = S3Client.builder()
                .endpointOverride(endpoint)
                .region(region)
                .credentialsProvider(credentialsProvider)
                .serviceConfiguration(configuration)
                .build();
        this.s3Presigner = S3Presigner.builder()
                .endpointOverride(endpoint)
                .region(region)
                .credentialsProvider(credentialsProvider)
                .serviceConfiguration(configuration)
                .build();
    }

    @Override
    public String generateUploadPresignedUrl(String fileKey, String contentType, long contentLength) {
        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(appProperties.storageBucket())
                    .key(fileKey)
                    .contentType(contentType)
                    .contentLength(contentLength)
                    .build();

            PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                    .signatureDuration(UPLOAD_URL_EXPIRE)
                    .putObjectRequest(putObjectRequest)
                    .build();

            return s3Presigner.presignPutObject(presignRequest).url().toString();
        } catch (RuntimeException ex) {
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR, "업로드 URL 생성 중 오류가 발생했습니다");
        }
    }

    @Override
    public String generateGetPresignedUrl(String fileKey, Duration expireDuration) {
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(appProperties.storageBucket())
                    .key(fileKey)
                    .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(expireDuration)
                    .getObjectRequest(getObjectRequest)
                    .build();

            return s3Presigner.presignGetObject(presignRequest).url().toString();
        } catch (RuntimeException ex) {
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR, "다운로드 URL 생성 중 오류가 발생했습니다");
        }
    }

    @Override
    public StorageObjectMeta headObject(String fileKey) {
        try {
            HeadObjectRequest request = HeadObjectRequest.builder()
                    .bucket(appProperties.storageBucket())
                    .key(fileKey)
                    .build();
            HeadObjectResponse response = s3Client.headObject(request);
            return new StorageObjectMeta(response.contentLength(), response.contentType());
        } catch (NoSuchKeyException ex) {
            return null;
        } catch (S3Exception ex) {
            String errorCode = ex.awsErrorDetails() == null ? null : ex.awsErrorDetails().errorCode();
            if (ex.statusCode() == 404
                    || "404".equals(errorCode)
                    || "NotFound".equals(errorCode)
                    || "NoSuchKey".equals(errorCode)) {
                return null;
            }
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR, "스토리지 객체 조회 중 오류가 발생했습니다");
        } catch (RuntimeException ex) {
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR, "스토리지 객체 조회 중 오류가 발생했습니다");
        }
    }

    @Override
    public StorageObjectListPage listObjects(String prefix, String continuationToken, int maxKeys) {
        try {
            ListObjectsV2Request.Builder requestBuilder = ListObjectsV2Request.builder()
                    .bucket(appProperties.storageBucket())
                    .prefix(prefix)
                    .maxKeys(maxKeys);
            if (continuationToken != null && !continuationToken.isBlank()) {
                requestBuilder.continuationToken(continuationToken);
            }

            ListObjectsV2Response response = s3Client.listObjectsV2(requestBuilder.build());
            return new StorageObjectListPage(
                    response.contents().stream().map(S3Object::key).toList(),
                    response.nextContinuationToken()
            );
        } catch (RuntimeException ex) {
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR, "스토리지 객체 목록 조회 중 오류가 발생했습니다");
        }
    }

    @Override
    public byte[] getObject(String fileKey) {
        try {
            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(appProperties.storageBucket())
                    .key(fileKey)
                    .build();
            ResponseBytes<GetObjectResponse> response = s3Client.getObjectAsBytes(request);
            return response.asByteArray();
        } catch (NoSuchKeyException ex) {
            throw new AppException(ErrorCode.NOT_FOUND, "스토리지 객체를 찾을 수 없습니다");
        } catch (S3Exception ex) {
            String errorCode = ex.awsErrorDetails() == null ? null : ex.awsErrorDetails().errorCode();
            if (ex.statusCode() == 404
                    || "404".equals(errorCode)
                    || "NotFound".equals(errorCode)
                    || "NoSuchKey".equals(errorCode)) {
                throw new AppException(ErrorCode.NOT_FOUND, "스토리지 객체를 찾을 수 없습니다");
            }
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR, "스토리지 객체 조회 중 오류가 발생했습니다");
        } catch (RuntimeException ex) {
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR, "스토리지 객체 조회 중 오류가 발생했습니다");
        }
    }

    @Override
    public void putObject(String fileKey, byte[] content, String contentType) {
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(appProperties.storageBucket())
                    .key(fileKey)
                    .contentType(contentType)
                    .contentLength((long) content.length)
                    .build();
            s3Client.putObject(request, RequestBody.fromBytes(content));
        } catch (RuntimeException ex) {
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR, "스토리지 객체 업로드 중 오류가 발생했습니다");
        }
    }

    @Override
    public StorageDeleteResult deleteObjects(List<String> fileKeys) {
        if (fileKeys == null || fileKeys.isEmpty()) {
            return new StorageDeleteResult(List.of(), List.of());
        }

        try {
            DeleteObjectsRequest request = DeleteObjectsRequest.builder()
                    .bucket(appProperties.storageBucket())
                    .delete(Delete.builder()
                            .objects(fileKeys.stream()
                                    .map(fileKey -> ObjectIdentifier.builder().key(fileKey).build())
                                    .toList())
                            .build())
                    .build();
            DeleteObjectsResponse response = s3Client.deleteObjects(request);
            return new StorageDeleteResult(
                    response.deleted().stream().map(DeletedObject::key).toList(),
                    response.errors().stream().map(S3Error::key).toList()
            );
        } catch (RuntimeException ex) {
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR, "스토리지 객체 일괄 삭제 중 오류가 발생했습니다");
        }
    }

    @Override
    public void deleteObject(String fileKey) {
        try {
            DeleteObjectRequest request = DeleteObjectRequest.builder()
                    .bucket(appProperties.storageBucket())
                    .key(fileKey)
                    .build();
            s3Client.deleteObject(request);
        } catch (S3Exception ex) {
            String errorCode = ex.awsErrorDetails() == null ? null : ex.awsErrorDetails().errorCode();
            if (ex.statusCode() == 404
                    || "404".equals(errorCode)
                    || "NotFound".equals(errorCode)
                    || "NoSuchKey".equals(errorCode)) {
                return;
            }
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR, "스토리지 객체 삭제 중 오류가 발생했습니다");
        } catch (RuntimeException ex) {
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR, "스토리지 객체 삭제 중 오류가 발생했습니다");
        }
    }

    @PreDestroy
    public void closeClients() {
        s3Client.close();
        s3Presigner.close();
    }
}
