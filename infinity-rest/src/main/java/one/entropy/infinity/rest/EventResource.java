package one.entropy.infinity.rest;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import one.entropy.infinity.rest.dto.EventDto;
import one.entropy.infinity.rest.storage.Event;
import one.entropy.infinity.rest.storage.EventService;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Path("/events")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class EventResource {

    @Inject
    @Channel("events")
    Emitter<EventDto> emitterForEvents;

    @POST
    @APIResponses(value = {
            @APIResponse(
                    responseCode = "200",
                    description = "Event published",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON))})
    @Operation( summary = "Publish new event to the system")
    public Uni<EventDto> add(EventDto eventDto) {
        return Uni.createFrom().completionStage(emitterForEvents.send(eventDto)).onItem().apply(x -> eventDto);
    }

    @Inject
    EventService eventService;

    @GET
    @Path("/{group}/{type}/{timestamp}")
    @APIResponses(value = {
            @APIResponse(
                    responseCode = "200",
                    description = "Events retrieved",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON))})
    @Operation( summary = "Get event list")
    @Parameter(name = "group", description = "Group of events", required = true, schema = @Schema(type = SchemaType.STRING))
    @Parameter(name = "type", description = "Type of events", required = true, schema = @Schema(type = SchemaType.STRING))
    @Parameter(name = "timestamp", description = "Select events before the parameter value",example = "2222-08-12T15:52:42.942Z", required = true, schema = @Schema(type = SchemaType.STRING))
    public Multi<EventDto> getEvents(@PathParam("group") String group,  @PathParam("type") String type, @PathParam("timestamp") String timestamp) {
        Instant instant = Instant.parse(timestamp);
        return eventService
                .get(group, type, instant)
                .map(e -> new EventDto(e.getGroup(), e.getType(), LocalDateTime.ofInstant(e.getTimestamp(), ZoneId.systemDefault()), e.getValue()));
    }
}
