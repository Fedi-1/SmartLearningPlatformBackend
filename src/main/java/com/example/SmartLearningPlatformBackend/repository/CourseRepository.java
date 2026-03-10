package com.example.SmartLearningPlatformBackend.repository;

import com.example.SmartLearningPlatformBackend.models.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {

    Optional<Course> findByDocumentId(Long documentId);

    List<Course> findByStudentId(Long studentId);

    @Query("SELECT c.category, COUNT(c) FROM Course c WHERE c.category IS NOT NULL GROUP BY c.category ORDER BY COUNT(c) DESC")
    List<Object[]> countGroupedByCategory();
}
