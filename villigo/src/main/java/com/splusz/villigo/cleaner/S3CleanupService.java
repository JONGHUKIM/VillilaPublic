package com.splusz.villigo.cleaner;

import org.springframework.beans.factory.annotation.Value;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.Delete;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;

import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class S3CleanupService {
    private final S3Client s3Client;
    private final String bucket;

    // prod.env 파일은 절대로 삭제하지 않도록 제외 목록에 추가
    private static final String EXCLUDE_KEY = "prod.env";

    public S3CleanupService(S3Client s3Client, @Value("${aws.s3.bucket}") String bucket) {
        this.s3Client = s3Client;
        this.bucket = bucket;
    }

    public Set<String> listAllKeys(String prefix) {
        Set<String> keys = new HashSet<>();
        String continuationToken = null;
        ListObjectsV2Response resp;

        do {
            ListObjectsV2Request.Builder req = ListObjectsV2Request.builder()
                .bucket(bucket)
                .prefix(prefix)
                .maxKeys(1000);
            if (continuationToken != null)
                req.continuationToken(continuationToken);

            resp = s3Client.listObjectsV2(req.build());
            for (S3Object obj : resp.contents()) {
                String key = obj.key();
                if (!EXCLUDE_KEY.equals(key)) {
                    keys.add(key);
                }
            }
            continuationToken = resp.nextContinuationToken();
        } while (resp.isTruncated());

        return keys;
    }


    public void deleteKey(String key) {
        s3Client.deleteObject(b -> b.bucket(bucket).key(key));
    }

    public void deleteKeysBatch(List<String> keysToDelete) {
        if (keysToDelete.isEmpty()) {
            return;
        }

        final int BATCH_SIZE = 1000;
        for (int i = 0; i < keysToDelete.size(); i += BATCH_SIZE) {
            List<String> currentBatchKeys = keysToDelete.subList(i, Math.min(i + BATCH_SIZE, keysToDelete.size()));

            List<ObjectIdentifier> objects = currentBatchKeys.stream()
                    .map(key -> ObjectIdentifier.builder().key(key).build())
                    .collect(Collectors.toList());

            DeleteObjectsRequest deleteRequest = DeleteObjectsRequest.builder()
                    .bucket(bucket)
                    .delete(Delete.builder().objects(objects).build())
                    .build();

            s3Client.deleteObjects(deleteRequest);
        }
    }
}