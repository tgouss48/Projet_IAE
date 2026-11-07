package ma.mobility.booking.domain;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboxRepository extends JpaRepository<OutboxEvent, Long> {

    List<OutboxEvent> findTop50ByPublishedAtIsNullOrderByIdAsc();

    List<OutboxEvent> findByAggregateIdOrderByIdAsc(String aggregateId);
}
