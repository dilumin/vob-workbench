package com.synergen.vobworkbench.dto;

import java.util.List;

public final class CommonDtos {
    private CommonDtos() {
    }

    public record PageInfo(int page, int size, long totalElements, int totalPages) {
    }

    public record PageResponse<T>(List<T> content, PageInfo page) {
    }
}
