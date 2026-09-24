package com.kbase.backend.repository;

import com.kbase.backend.entity.Document;
import com.kbase.backend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DocumentRepository extends JpaRepository<Document, Long> {

    List<Document> findByUploadedByOrderByCreatedAtDesc(User user);

    // Tìm kiếm theo tên file (không phân biệt hoa thường) kết hợp phân trang
    @Query("SELECT d FROM Document d WHERE d.uploadedBy = :user AND " +
            "(:keyword IS NULL OR LOWER(d.originalFileName) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Document> searchMyDocuments(@Param("user") User user,
            @Param("keyword") String keyword,
            Pageable pageable);
}