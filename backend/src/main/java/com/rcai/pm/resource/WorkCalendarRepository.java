package com.rcai.pm.resource; import org.springframework.data.jpa.repository.JpaRepository; import java.time.LocalDate; import java.util.List;
public interface WorkCalendarRepository extends JpaRepository<WorkCalendarDay,LocalDate>{List<WorkCalendarDay> findByDateBetweenOrderByDate(LocalDate from,LocalDate to);}
