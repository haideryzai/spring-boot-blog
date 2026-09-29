package com.blogapp.blogapp.repository;

import com.blogapp.blogapp.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostRepository extends JpaRepository<Post, Long> {

    // Every filter is optional: a null parameter means "don't filter on this"
    @EntityGraph(attributePaths = {"author", "category"})
    @Query("""
            SELECT p FROM Post p
            WHERE (:categoryId IS NULL OR p.category.id = :categoryId)
              AND (:authorId IS NULL OR p.author.id = :authorId)
              AND (:search IS NULL
                   OR LOWER(p.title) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
                   OR LOWER(p.content) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
            """)
    Page<Post> search(@Param("categoryId") Long categoryId,
                      @Param("authorId") Long authorId,
                      @Param("search") String search,
                      Pageable pageable);

    @Modifying
    @Query("UPDATE Post p SET p.category = NULL WHERE p.category.id = :categoryId")
    void clearCategory(@Param("categoryId") Long categoryId);
}
