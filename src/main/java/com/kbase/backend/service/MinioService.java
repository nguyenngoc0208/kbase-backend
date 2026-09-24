package com.kbase.backend.service;

import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.UUID;

@Service
public class MinioService {

    private final MinioClient minioClient;

    @Value("${minio.bucket-name}")
    private String bucketName;

    public MinioService(MinioClient minioClient) {
        this.minioClient = minioClient;
    }

    public String uploadFile(MultipartFile file) {
        try {
            boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!found) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            }

            String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
            try (InputStream inputStream = file.getInputStream()) {
                minioClient.putObject(
                        PutObjectArgs.builder()
                                .bucket(bucketName)
                                .object(fileName)
                                .stream(inputStream, file.getSize(), -1)
                                .contentType(file.getContentType())
                                .build());
            }
            return fileName;
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi tải file lên MinIO: " + e.getMessage());
        }
    }

    public InputStream downloadFile(String storedFileName) {
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(storedFileName)
                            .build());
        } catch (Exception e) {
            throw new RuntimeException("Không thể lấy file từ MinIO: " + e.getMessage());
        }
    }

    public void deleteFile(String storedFileName) {
        try {
            minioClient.removeObject(
                    io.minio.RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(storedFileName)
                            .build());
        } catch (Exception e) {
            throw new RuntimeException("Không thể xóa file trên MinIO: " + e.getMessage());
        }
    }
}