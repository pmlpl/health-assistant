package com.example.healthassistant.repository;

import com.example.healthassistant.model.FitnessRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface FitnessRecordRepository extends JpaRepository<FitnessRecord, Long> {
    List<FitnessRecord> findByUserId(String userId);
    List<FitnessRecord> findByUserIdAndDate(String userId, LocalDate date);
    List<FitnessRecord> findByUserIdAndDateBetween(String userId, LocalDate startDate, LocalDate endDate);
    
    // 自定义查询：根据年月查询健身记录
    @org.springframework.data.jpa.repository.Query(
        "SELECT f FROM FitnessRecord f WHERE f.userId = :userId " +
        "AND YEAR(f.date) = :year AND MONTH(f.date) = :month"
    )
    List<FitnessRecord> findByUserIdAndYearMonth(
        @org.springframework.data.repository.query.Param("userId") String userId,
        @org.springframework.data.repository.query.Param("year") int year,
        @org.springframework.data.repository.query.Param("month") int month
    );
}
