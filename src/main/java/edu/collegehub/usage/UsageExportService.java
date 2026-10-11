package edu.collegehub.usage;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsageExportService {
    private final UsageEventRepository events;
    public UsageExportService(UsageEventRepository events) { this.events = events; }
    @Transactional(readOnly = true)
    public List<UsageExportRow> rows() {
        return events.findAllByOrderByEventAtAsc().stream().map(event -> {
            var resource = event.getResource();
            return new UsageExportRow(event.getEventType(), resource.getProgramme().getCode(),
                    resource.getSubject().getCode(), resource.getSubject().getName(),
                    resource.getSemester().getNumber(), resource.getResourceType().getLabel(), event.getEventAt());
        }).toList();
    }
}
