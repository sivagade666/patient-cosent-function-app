package com.srk.domain.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ConsentItemDto {

    private String id;
    private String partyId;
    private String consentValue;
    private String state;
}
