package ma.mobility.gateway.search;

import java.util.List;

public record JourneySearchResponse(String origin, String via, String destination, String date,
                                    List<SegmentResult> segments) {
}
