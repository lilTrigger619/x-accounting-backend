package com.unionsg.xaccounting.MapperLayer;

import com.unionsg.xaccounting.dto.CreatedByDTO;
import com.unionsg.xaccounting.entity.User.User;

public class CreatedByMapper {

    private CreatedByMapper() {
        throw new UnsupportedOperationException("Mapper class cannot be instantiated");
    }

    public static CreatedByDTO toDto(User user) {
        if (user == null) {
            return null;
        }

        String first = user.getFirstName() != null ? user.getFirstName() : "";
        String last = user.getLastName() != null ? user.getLastName() : "";
        String fullName = (first + " " + last).trim();
        if (fullName.isEmpty()) {
            fullName = user.getEmail();
        }

        return CreatedByDTO.builder()
                .id(user.getId())
                .fullName(fullName)
                .build();
    }
}
