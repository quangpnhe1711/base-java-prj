package com.luvina.base.template.dto.response;

import java.util.List;

import com.luvina.base.core.dto.BasePageResponse;

import lombok.Getter;
import lombok.Setter;

/**
 * One page of search results.
 *
 * <p>The content field is named after the domain rather than {@code content},
 * which is the convention across our services.
 */
@Getter
@Setter
public class SamplePageResponse extends BasePageResponse {

    /** Samples on the current page. */
    private List<SampleResponse> sampleList;
}
