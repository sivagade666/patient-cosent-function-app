package com.srk.domain;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ConsentItem {
    private String id;
    private String partyId;
    private String consentValue;
    private String state;
}
