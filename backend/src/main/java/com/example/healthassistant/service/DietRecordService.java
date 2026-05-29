package com.example.healthassistant.service;

import com.example.healthassistant.dto.DietRecordDto;
import com.example.healthassistant.model.DietRecord;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface DietRecordService {
    DietRecord saveDietRecord(DietRecordDto dietRecordDto);
    List<DietRecord> getDailyRecords(String userId, LocalDate date);
    List<DietRecord> getWeeklyRecords(String userId, LocalDate weekStart);
    List<DietRecord> getRecordsInRange(String userId, LocalDate startDate, LocalDate endDate);
    Map<String, Object> getMonthlySummary(String userId, int year, int month);
    void deleteDietRecord(Long id);
    int deleteBatchDietRecords(List<Long> ids);
}