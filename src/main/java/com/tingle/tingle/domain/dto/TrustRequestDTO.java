package com.tingle.tingle.domain.dto;

import java.util.List;

public class TrustRequestDTO {

    private List<String> serials;

    public TrustRequestDTO() {}

    public TrustRequestDTO(List<String> serials) {
        this.serials = serials;
    }

    public List<String> getSerials() {
        return serials;
    }

    public void setSerials(List<String> serials) {
        this.serials = serials;
    }
}
