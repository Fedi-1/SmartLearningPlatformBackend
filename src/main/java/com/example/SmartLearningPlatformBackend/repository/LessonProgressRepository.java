package com.example.SmartLearningPlatformBackend.repository;

import com.example.SmartLearningPlatformBackend.models.LessonProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface LessonProgressRepository extends JpaRepository<LessonProgress, Long> {

  Optional<LessonProgress> findByStudentIdAndLessonId(Long studentId, Long lessonId);

  List<LessonProgress> findByStudentIdAndLessonIdIn(Long studentId, List<Long> lessonIds);

  int countByStudentIdAndIsCompleted(Long studentId, Boolean isCompleted);

  /**
   * Returns one LessonProgress per (studentId, courseId) pair where:
   * - the student has at least one incomplete lesson in that course
   * - the most recent lastAccessedAt across all their lessons in that course
   * is older than the given cutoff (i.e. inactive for N days)
   */
  @Query("""
      SELECT lp FROM LessonProgress lp
      JOIN Lesson l ON l.id = lp.lessonId
      WHERE lp.isCompleted = false
        AND lp.isLocked = false
        AND (
          SELECT MAX(lp2.lastAccessedAt)
          FROM LessonProgress lp2
          JOIN Lesson l2 ON l2.id = lp2.lessonId
          WHERE lp2.studentId = lp.studentId
            AND l2.courseId = l.courseId
        ) < :cutoff
      GROUP BY lp.studentId, l.courseId, lp.id
      """)
  List<LessonProgress> findInactiveInProgressLessons(@Param("cutoff") LocalDateTime cutoff);
}
