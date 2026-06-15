package com.phyriak.notification_orchestrator.mapper;

import com.phyriak.newsletter.model.NewsLetterSignupRequest;
import com.phyriak.notification_orchestrator.model.NotificationEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface NotificationMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "type", constant = "NEWSLETTER")
    @Mapping(target = "status", constant = "NEW")
    NotificationEntity toEntity(NewsLetterSignupRequest request);

}