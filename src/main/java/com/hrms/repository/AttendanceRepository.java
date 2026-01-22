package com.hrms.repository;

import com.hrms.entity.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    @Query(value = "SELECT * FROM tb_attendance_details WHERE emp_id = :employeeId AND attendance_date = :date", nativeQuery = true)
    Optional<Attendance> findDailyAttendance(Integer employeeId, LocalDate date);

    @Query(value = "SELECT MONTH(attendance_date) as month, COUNT(*) as daysPresent, SUM(effective_wh) as totalHours " +
            "FROM tb_attendance_details " +
            "WHERE emp_id = :empId AND YEAR(attendance_date) = :year " +
            "GROUP BY MONTH(attendance_date)", nativeQuery = true)
    List<Map<String, Object>> findMonthlySummary(Integer empId, int year);

    // Daily Details for a Specific Month
    @Query(value = "SELECT * FROM tb_attendance_details " +
            "WHERE emp_id = :empId " +
            "AND MONTH(attendance_date) = :month " +
            "AND YEAR(attendance_date) = :year " +
            "ORDER BY attendance_date ASC", nativeQuery = true)
    List<Attendance> findDailyAttendanceByMonth(Integer empId, int month, int year);

}




//SELECT
//MONTH(attendance_date) AS MONTH,
//COUNT(*) AS daysPresent,
//SUM(effective_wh) AS totalHours
//FROM tb_attendance_details
//WHERE emp_id=2
//AND YEAR(attendance_date) =2026
//GROUP BY MONTH(attendance_date);




//SELECT * FROM tb_attendance_details
//WHERE emp_id=1
//AND MONTH(attendance_date)=1
//AND YEAR(attendance_date)=2026
//ORDER BY attendance_date ASC;