package com.unionsg.xaccounting.documenttemplate.dto.request;

import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UpdateDesignRequest {

    /**
     * Accepts CSS hex colors (#RGB/#RRGGBB/#RRGGBBAA), rgb()/rgba(), hsl()/hsla(), or a
     * plain CSS named color (letters only). These values are rendered unescaped into a
     * document's inline {@code <style>} block, so the format is restricted to prevent
     * CSS/HTML injection.
     */
    private static final String COLOR_PATTERN =
            "^(#[0-9A-Fa-f]{3,8}|rgba?\\(\\s*\\d{1,3}%?\\s*,\\s*\\d{1,3}%?\\s*,\\s*\\d{1,3}%?\\s*(,\\s*(0|1|0?\\.\\d+)\\s*)?\\)"
                    + "|hsla?\\(\\s*\\d{1,3}\\s*,\\s*\\d{1,3}%\\s*,\\s*\\d{1,3}%\\s*(,\\s*(0|1|0?\\.\\d+)\\s*)?\\)|[A-Za-z]+)$";

    private static final String FONT_FAMILY_PATTERN = "^[A-Za-z0-9 ,'\"-]+$";

    private Long logoFileId;

    private String logoPosition;

    private Integer logoWidth;

    private Integer logoHeight;

    @Pattern(regexp = COLOR_PATTERN, message = "primaryColor must be a valid hex, rgb()/rgba(), hsl()/hsla(), or named CSS color")
    private String primaryColor;

    @Pattern(regexp = COLOR_PATTERN, message = "secondaryColor must be a valid hex, rgb()/rgba(), hsl()/hsla(), or named CSS color")
    private String secondaryColor;

    @Pattern(regexp = FONT_FAMILY_PATTERN, message = "fontFamily may only contain letters, digits, spaces, commas, hyphens, and quotes")
    private String fontFamily;

    private Integer fontSize;

    @Pattern(regexp = COLOR_PATTERN, message = "fontColor must be a valid hex, rgb()/rgba(), hsl()/hsla(), or named CSS color")
    private String fontColor;
}

