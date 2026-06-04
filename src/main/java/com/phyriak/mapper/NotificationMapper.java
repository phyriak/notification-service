package com.phyriak.mapper;

import com.phyriak.model.NotificationEntity;
import com.phyriak.newsletter.NewsLetterSignupEvent;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface NotificationMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "type", constant = "NEWSLETTER_SIGNUP")
    NotificationEntity toEntity(NewsLetterSignupEvent event);

}